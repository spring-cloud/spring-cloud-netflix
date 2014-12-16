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
import org.springframework.cloud.netflix.zuul.ProxyRouteLocator.ProxyRouteSpec;
import org.springframework.cloud.netflix.zuul.ZuulProperties.ZuulRoute;
import org.springframework.core.env.ConfigurableEnvironment;

import com.google.common.collect.Lists;

/**
 * @author Spencer Gibb
 * @author Dave Syer
 */
public class ProxyRouteLocatorTests {

	public static final String IGNOREDSERVICE = "ignoredservice";
	public static final String ASERVICE = "aservice";
	public static final String MYSERVICE = "myservice";

	@Mock
	ConfigurableEnvironment env;

	@Mock
	DiscoveryClient discovery;

	private ZuulProperties properties = new ZuulProperties();

	@Before
	public void init() {
		initMocks(this);
	}
	
	@Test
	public void testGetMatchingPath() throws Exception {
		ProxyRouteLocator routeLocator = new ProxyRouteLocator(this.discovery, this.properties);
		this.properties.getRoutes().put("foo", new ZuulRoute("/foo/**"));
		properties.init();
		routeLocator.getRoutes(); // force refresh
		ProxyRouteSpec route = routeLocator.getMatchingRoute("/foo/1");
		assertEquals("foo", route.getLocation());
		assertEquals("foo", route.getId());
	}

	@Test
	public void testGetMatchingPathWithPrefix() throws Exception {
		ProxyRouteLocator routeLocator = new ProxyRouteLocator(this.discovery, this.properties);
		this.properties.getRoutes().put("foo", new ZuulRoute("/foo/**"));
		this.properties.setPrefix("/proxy");
		properties.init();
		routeLocator.getRoutes(); // force refresh
		ProxyRouteSpec route = routeLocator.getMatchingRoute("/proxy/foo/1");
		assertEquals("foo", route.getLocation());
		assertEquals("/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithNoPrefixStripping() throws Exception {
		ProxyRouteLocator routeLocator = new ProxyRouteLocator(this.discovery, this.properties);
		this.properties.getRoutes().put("foo", new ZuulRoute("foo", "/foo/**", "foo", null, false));
		this.properties.setStripPrefix(false);
		this.properties.setPrefix("/proxy");
		routeLocator.getRoutes(); // force refresh
		ProxyRouteSpec route = routeLocator.getMatchingRoute("/proxy/foo/1");
		assertEquals("foo", route.getLocation());
		assertEquals("/proxy/foo/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithLocalPrefixStripping() throws Exception {
		ProxyRouteLocator routeLocator = new ProxyRouteLocator(this.discovery, this.properties);
		this.properties.getRoutes().put("foo", new ZuulRoute("/foo/**", "foo"));
		this.properties.setStripPrefix(false);
		this.properties.setPrefix("/proxy");
		routeLocator.getRoutes(); // force refresh
		ProxyRouteSpec route = routeLocator.getMatchingRoute("/proxy/foo/1");
		assertEquals("foo", route.getLocation());
		assertEquals("/proxy/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithGlobalPrefixStripping() throws Exception {
		ProxyRouteLocator routeLocator = new ProxyRouteLocator(this.discovery, this.properties);
		this.properties.getRoutes().put("foo", new ZuulRoute("foo", "/foo/**", "foo", null, false));
		this.properties.setPrefix("/proxy");
		routeLocator.getRoutes(); // force refresh
		ProxyRouteSpec route = routeLocator.getMatchingRoute("/proxy/foo/1");
		assertEquals("foo", route.getLocation());
		assertEquals("/foo/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithRoutePrefixStripping() throws Exception {
		ProxyRouteLocator routeLocator = new ProxyRouteLocator(this.discovery, this.properties);
		ZuulRoute zuulRoute = new ZuulRoute("/foo/**");
		zuulRoute.setStripPrefix(true);
		this.properties.getRoutes().put("foo", zuulRoute);
		properties.init();
		routeLocator.getRoutes(); // force refresh
		ProxyRouteSpec route = routeLocator.getMatchingRoute("/foo/1");
		assertEquals("foo", route.getLocation());
		assertEquals("/1", route.getPath());
	}

	@Test
	public void testGetRoutes() {
		ProxyRouteLocator routeLocator = new ProxyRouteLocator(this.discovery, this.properties);
		this.properties.getRoutes().put(ASERVICE, new ZuulRoute("/"+ASERVICE + "/**"));
		properties.init();

		Map<String, String> routesMap = routeLocator.getRoutes();

		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertMapping(routesMap, ASERVICE);
	}

	@Test
	public void testGetRoutesWithMapping() {
		ProxyRouteLocator routeLocator = new ProxyRouteLocator(this.discovery, this.properties);
		this.properties.getRoutes().put(ASERVICE, new ZuulRoute("/"+ASERVICE + "/**", ASERVICE));
		this.properties.setPrefix("/foo");

		Map<String, String> routesMap = routeLocator.getRoutes();
		assertMapping(routesMap, ASERVICE, "foo/" + ASERVICE);
	}

	@Test
	public void testGetPhysicalRoutes() {
		ProxyRouteLocator routeLocator = new ProxyRouteLocator(this.discovery, this.properties);
		this.properties.getRoutes().put(ASERVICE, new ZuulRoute("/"+ASERVICE + "/**", "http://" + ASERVICE));

		Map<String, String> routesMap = routeLocator.getRoutes();

		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertMapping(routesMap, "http://" + ASERVICE, ASERVICE);
	}

	@Test
	public void testGetDefaultRoute() {
		ProxyRouteLocator routeLocator = new ProxyRouteLocator(this.discovery, this.properties);
		this.properties.getRoutes().put(ASERVICE, new ZuulRoute("/**", ASERVICE));

		Map<String, String> routesMap = routeLocator.getRoutes();

		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertDefaultMapping(routesMap, ASERVICE);
	}

	@Test
	public void testGetDefaultPhysicalRoute() {
		ProxyRouteLocator routeLocator = new ProxyRouteLocator(this.discovery, this.properties);
		this.properties.getRoutes().put(ASERVICE, new ZuulRoute("/**", "http://" + ASERVICE));

		Map<String, String> routesMap = routeLocator.getRoutes();

		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertDefaultMapping(routesMap, "http://" + ASERVICE);
	}

	@Test
	public void testIgnoreRoutes() {
		ProxyRouteLocator routeLocator = new ProxyRouteLocator(this.discovery, this.properties);
		this.properties.setIgnoredServices(Lists.newArrayList(IGNOREDSERVICE));

		when(discovery.getServices()).thenReturn(
				Lists.newArrayList(IGNOREDSERVICE));

		Map<String, String> routesMap = routeLocator.getRoutes();
		String serviceId = routesMap.get(getMapping(IGNOREDSERVICE));
		assertNull("routes did not ignore " + IGNOREDSERVICE, serviceId);
	}

	@Test
	public void testAutoRoutes() {
		ProxyRouteLocator routeLocator = new ProxyRouteLocator(this.discovery, this.properties);

		when(discovery.getServices()).thenReturn(
				Lists.newArrayList(MYSERVICE));

		Map<String, String> routesMap = routeLocator.getRoutes();

		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertMapping(routesMap, MYSERVICE);
	}

	@Test
	public void testAutoRoutesCanBeOverridden() {
		this.properties.getRoutes().put(MYSERVICE, new ZuulRoute("/"+MYSERVICE + "/**", "http://example.com/" + MYSERVICE));
		ProxyRouteLocator routeLocator = new ProxyRouteLocator(this.discovery, this.properties);

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

	protected void assertDefaultMapping(Map<String, String> routesMap, String expectedRoute) {
		String mapping = "/**";
		String route = routesMap.get(mapping);
		assertEquals("routesMap had wrong value for " + mapping, expectedRoute,
				route);
	}
}
