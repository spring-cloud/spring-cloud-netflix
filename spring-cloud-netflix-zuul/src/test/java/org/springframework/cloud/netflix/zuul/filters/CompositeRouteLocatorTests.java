package org.springframework.cloud.netflix.zuul.filters;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

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
	public void test_getIgnoredPaths() {
		assertThat(locator.getIgnoredPaths(), hasItems("ign1", "ign2"));

	}

	@Test
	public void test_getRoutes() {
		assertThat(locator.getRoutes(),
				hasItems(createRoute("1", "/pathA"), createRoute("2", "/pathB")));
	}

	@Test
	public void test_getMatchingRoute() {
		assertThat(locator.getMatchingRoute("/pathA"), notNullValue());
		assertThat(locator.getMatchingRoute("/pathA").getId(), is("1"));
		assertThat("Locator 1 should take precedence", locator.getMatchingRoute("/pathB").getId(),
				is("2"));
		assertThat(locator.getMatchingRoute("/pathNot"), nullValue());
	}

	@Test
	public void test_refresh() {
		RefreshableRouteLocator mock = mock(RefreshableRouteLocator.class);
		new CompositeRouteLocator(asList(mock)).refresh();
		verify(mock).refresh();
	}

	private Route createRoute(String id, String path) {
		return new Route(id, path, null, null, false, Collections.<String>emptySet());
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