package org.springframework.cloud.netflix.zuul.filters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;

/**
 *
 * @author Johannes Edmeier
 *
 */
public class SimpleRouteLocatorTests {
	public static final String ASERVICE = "aservice";

	private ZuulProperties properties = new ZuulProperties();

	@Test
	public void testGetDefaultPhysicalRoute() {
		SimpleRouteLocator routeLocator = new SimpleRouteLocator("/", this.properties);
		this.properties.getRoutes().put(ASERVICE,
				new ZuulRoute("/**", "http://" + ASERVICE));
		List<Route> routesMap = routeLocator.getRoutes();
		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertDefaultMapping(routesMap, "http://" + ASERVICE);
	}

	@Test
	public void testGetPhysicalRoutes() {
		SimpleRouteLocator routeLocator = new SimpleRouteLocator("/", this.properties);
		this.properties.getRoutes().put(ASERVICE,
				new ZuulRoute("/" + ASERVICE + "/**", "http://" + ASERVICE));
		List<Route> routesMap = routeLocator.getRoutes();
		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertMapping(routesMap, "http://" + ASERVICE, ASERVICE);
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

	protected void assertDefaultMapping(List<Route> routesMap, String expectedRoute) {
		String mapping = "/**";
		String route = getRoute(routesMap, mapping).getLocation();
		assertEquals("routesMap had wrong value for " + mapping, expectedRoute, route);
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
}
