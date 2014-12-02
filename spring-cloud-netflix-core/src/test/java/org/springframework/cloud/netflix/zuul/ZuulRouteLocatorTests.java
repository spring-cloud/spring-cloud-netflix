package org.springframework.cloud.netflix.zuul;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.ZuulProperties.ZuulRoute;
import org.springframework.core.env.ConfigurableEnvironment;

import com.google.common.collect.Lists;

/**
 * @author Spencer Gibb
 * @author Dave Syer
 */
public class ZuulRouteLocatorTests {

	public static final String IGNOREDSERVICE = "ignoredservice";
	public static final String ASERVICE = "aservice";
	public static final String MYSERVICE = "myservice";

	@Mock
	ConfigurableEnvironment env;

	@Mock
	DiscoveryClient discovery;

	@Before
	public void init() {
		initMocks(this);
	}

	@Test
	public void testGetRoutes() {
		ZuulProperties properties = new ZuulProperties();
		ZuulRouteLocator routeLocator = new ZuulRouteLocator(this.discovery, properties);
		properties.getRoutes().put(ASERVICE, new ZuulRoute("/"+ASERVICE + "/**"));

		Map<String, String> routesMap = routeLocator.getRoutes();

		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertMapping(routesMap, ASERVICE);
	}

	@Test
	public void testGetRoutesWithMapping() {
		ZuulProperties properties = new ZuulProperties();
		ZuulRouteLocator routeLocator = new ZuulRouteLocator(this.discovery, properties);
		properties.getRoutes().put(ASERVICE, new ZuulRoute("/"+ASERVICE + "/**", ASERVICE));
		// Prefix doesn't have any impact on the routes (it's used in the filter)
		properties.setPrefix("/foo");

		Map<String, String> routesMap = routeLocator.getRoutes();

		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertMapping(routesMap, ASERVICE);
	}

	@Test
	public void testGetPhysicalRoutes() {
		ZuulProperties properties = new ZuulProperties();
		ZuulRouteLocator routeLocator = new ZuulRouteLocator(this.discovery, properties);
		properties.getRoutes().put(ASERVICE, new ZuulRoute("/"+ASERVICE + "/**", "http://" + ASERVICE));

		Map<String, String> routesMap = routeLocator.getRoutes();

		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertMapping(routesMap, "http://" + ASERVICE, ASERVICE);
	}

	@Test
	public void testIgnoreRoutes() {
		ZuulProperties properties = new ZuulProperties();
		ZuulRouteLocator routeLocator = new ZuulRouteLocator(this.discovery, properties);
		properties.setIgnoredServices(Lists.newArrayList(IGNOREDSERVICE));

		when(discovery.getServices()).thenReturn(
				Lists.newArrayList(IGNOREDSERVICE));

		Map<String, String> routesMap = routeLocator.getRoutes();
		String serviceId = routesMap.get(getMapping(IGNOREDSERVICE));
		assertNull("routes did not ignore " + IGNOREDSERVICE, serviceId);
	}

	@Test
	public void testAutoRoutes() {
		ZuulProperties properties = new ZuulProperties();
		ZuulRouteLocator routeLocator = new ZuulRouteLocator(this.discovery, properties);

		when(discovery.getServices()).thenReturn(
				Lists.newArrayList(MYSERVICE));

		Map<String, String> routesMap = routeLocator.getRoutes();

		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertMapping(routesMap, MYSERVICE);
	}

	@Test
	public void testAutoRoutesCanBeOverridden() {
		ZuulProperties properties = new ZuulProperties();
		properties.getRoutes().put(MYSERVICE, new ZuulRoute("/"+MYSERVICE + "/**", "http://example.com/" + MYSERVICE));
		ZuulRouteLocator routeLocator = new ZuulRouteLocator(this.discovery, properties);

		when(discovery.getServices()).thenReturn(
				Lists.newArrayList(MYSERVICE));

		Map<String, String> routesMap = routeLocator.getRoutes();

		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertMapping(routesMap, "http://example.com/" + MYSERVICE, MYSERVICE);
	}

	protected void assertMapping(Map<String, String> routesMap, String serviceId) {
		assertMapping(routesMap, serviceId, serviceId);
	}
	
	protected void assertMapping(Map<String, String> routesMap, String expectedRoute, String key) {
		String mapping = getMapping(key);
		String route = routesMap.get(mapping);
		assertEquals("routesMap had wrong value for " + mapping, expectedRoute,
				route);
	}

	private String getMapping(String serviceId) {
		return "/" + serviceId + "/**";
	}
}
