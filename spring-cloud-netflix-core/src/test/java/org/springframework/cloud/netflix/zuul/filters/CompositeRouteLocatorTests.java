package org.springframework.cloud.netflix.zuul.filters;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;
import org.springframework.cloud.netflix.zuul.filters.discovery.DiscoveryClientRouteLocator;

/**
 * @author Johannes Edmeier
 */
public class CompositeRouteLocatorTests {
	private CompositeRouteLocator locator;

	public CompositeRouteLocatorTests() {
		List<RouteLocator> locators = new ArrayList<>();
		locators.add(new TestRouteLocator(asList("ign1"),
				asList(createRoute("1", "/pathA"))));
		locators.add(
				new TestRouteLocator(asList("ign1", "ign2"),
						asList(createRoute("2", "/pathA"), createRoute("2", "/pathB"))));
		this.locator = new CompositeRouteLocator(locators);
	}

	@Test
	public void testGetIgnoredPaths() {
		assertThat(locator.getIgnoredPaths(), hasItems("ign1", "ign2"));

	}

	@Test
	public void testGetRoutes() {
		assertThat(locator.getRoutes(),
				hasItems(createRoute("1", "/pathA"), createRoute("2", "/pathB")));
	}

	@Test
	public void testGetMatchingRoute() {
		assertThat(locator.getMatchingRoute("/pathA"), notNullValue());
		assertThat(locator.getMatchingRoute("/pathA").getId(), is("1"));
		assertThat("Locator 1 should take precedence", locator.getMatchingRoute("/pathB").getId(),
				is("2"));
		assertThat(locator.getMatchingRoute("/pathNot"), nullValue());
	}

	@Test
	public void testRefresh() {
		RefreshableRouteLocator mock = mock(RefreshableRouteLocator.class);
		new CompositeRouteLocator(asList(mock)).refresh();
		verify(mock).refresh();
	}

	@Test
	public void testAutoRoutesCanBeOverridden() {
		DiscoveryClient discovery = Mockito.mock(DiscoveryClient.class);
		ZuulProperties properties = new ZuulProperties();
		ZuulRoute route = new ZuulRoute("/myService/**", "http://example.com/myService");
		properties.getRoutes().put("myService", route);
		RouteLocator routeLocator = new CompositeRouteLocator(
				Arrays.asList(new SimpleRouteLocator("/", properties),
						new DiscoveryClientRouteLocator("/", discovery, properties)));
		given(discovery.getServices()).willReturn(Collections.singletonList("myService"));
		List<Route> routesMap = routeLocator.getRoutes();
		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertMapping(routesMap, "http://example.com/myService", "myService");
	}

	private Route createRoute(String id, String path) {
		return new Route(id, path, null, null, false, Collections.<String>emptySet());
	}

	protected void assertMapping(List<Route> routesMap, String expectedRoute,
			String key) {
		String mapping = getMapping(key);
		Route route = getRoute(routesMap, mapping);
		assertNotNull("Could not find route for " + key, route);
		String location = route.getLocation();
		assertEquals("routesMap had wrong value for " + mapping, expectedRoute, location);
	}

	private String getMapping(String serviceId) {
		return "/" + serviceId + "/**";
	}

	private Route getRoute(List<Route> routes, String path) {
		for (Route route : routes) {
			String pattern = route.getFullPath();
			if (path.equals(pattern)) {
				return route;
			}
		}
		return null;
	}

	private static class TestRouteLocator implements RouteLocator {
		private Collection<String> ignoredPaths;
		private List<Route> routes;

		public TestRouteLocator(Collection<String> ignoredPaths, List<Route> routes) {
			this.ignoredPaths = ignoredPaths;
			this.routes = routes;
		}

		@Override
		public Collection<String> getIgnoredPaths() {
			return this.ignoredPaths;
		}

		@Override
		public List<Route> getRoutes() {
			return this.routes;
		}

		@Override
		public Route getMatchingRoute(String path) {
			for (Route route : routes) {
				if (path.startsWith(route.getPath())) {
					return route;
				}
			}
			return null;
		}
	}
}