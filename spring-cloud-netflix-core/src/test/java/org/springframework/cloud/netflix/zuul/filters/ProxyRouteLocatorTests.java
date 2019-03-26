/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.zuul.filters;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.filters.ProxyRouteLocator.ProxyRouteSpec;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Spencer Gibb
 * @author Dave Syer
 */
public class ProxyRouteLocatorTests {

	public static final String IGNOREDSERVICE = "ignoredservice";

	public static final String ASERVICE = "aservice";

	public static final String MYSERVICE = "myservice";

	@Mock
	private ConfigurableEnvironment env;

	@Mock
	private DiscoveryClient discovery;

	private ZuulProperties properties = new ZuulProperties();

	@Before
	public void init() {
		initMocks(this);
	}

	@Test
	public void testGetMatchingPath() throws Exception {
		ProxyRouteLocator routeLocator = new ProxyRouteLocator("/", this.discovery,
				this.properties);
		this.properties.getRoutes().put("foo", new ZuulRoute("/foo/**"));
		this.properties.init();
		routeLocator.getRoutes(); // force refresh
		ProxyRouteSpec route = routeLocator.getMatchingRoute("/foo/1");
		assertEquals("foo", route.getLocation());
		assertEquals("foo", route.getId());
	}

	@Test
	public void testGetMatchingPathWithPrefix() throws Exception {
		ProxyRouteLocator routeLocator = new ProxyRouteLocator("/", this.discovery,
				this.properties);
		this.properties.getRoutes().put("foo", new ZuulRoute("/foo/**"));
		this.properties.setPrefix("/proxy");
		this.properties.init();
		routeLocator.getRoutes(); // force refresh
		ProxyRouteSpec route = routeLocator.getMatchingRoute("/proxy/foo/1");
		assertEquals("foo", route.getLocation());
		assertEquals("/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithServletPath() throws Exception {
		ProxyRouteLocator routeLocator = new ProxyRouteLocator("/app", this.discovery,
				this.properties);
		this.properties.getRoutes().put("foo", new ZuulRoute("/foo/**"));
		this.properties.init();
		routeLocator.getRoutes(); // force refresh
		ProxyRouteSpec route = routeLocator.getMatchingRoute("/app/foo/1");
		assertEquals("foo", route.getLocation());
		assertEquals("/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithNoPrefixStripping() throws Exception {
		ProxyRouteLocator routeLocator = new ProxyRouteLocator("/", this.discovery,
				this.properties);
		this.properties.getRoutes().put("foo",
				new ZuulRoute("foo", "/foo/**", "foo", null, false, null));
		this.properties.setStripPrefix(false);
		this.properties.setPrefix("/proxy");
		routeLocator.getRoutes(); // force refresh
		ProxyRouteSpec route = routeLocator.getMatchingRoute("/proxy/foo/1");
		assertEquals("foo", route.getLocation());
		assertEquals("/proxy/foo/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithLocalPrefixStripping() throws Exception {
		ProxyRouteLocator routeLocator = new ProxyRouteLocator("/", this.discovery,
				this.properties);
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
		ProxyRouteLocator routeLocator = new ProxyRouteLocator("/", this.discovery,
				this.properties);
		this.properties.getRoutes().put("foo",
				new ZuulRoute("foo", "/foo/**", "foo", null, false, null));
		this.properties.setPrefix("/proxy");
		routeLocator.getRoutes(); // force refresh
		ProxyRouteSpec route = routeLocator.getMatchingRoute("/proxy/foo/1");
		assertEquals("foo", route.getLocation());
		assertEquals("/foo/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithRoutePrefixStripping() throws Exception {
		ProxyRouteLocator routeLocator = new ProxyRouteLocator("/", this.discovery,
				this.properties);
		ZuulRoute zuulRoute = new ZuulRoute("/foo/**");
		zuulRoute.setStripPrefix(true);
		this.properties.getRoutes().put("foo", zuulRoute);
		this.properties.init();
		routeLocator.getRoutes(); // force refresh
		ProxyRouteSpec route = routeLocator.getMatchingRoute("/foo/1");
		assertEquals("foo", route.getLocation());
		assertEquals("/1", route.getPath());
	}

	@Test
	public void testGetRoutes() {
		ProxyRouteLocator routeLocator = new ProxyRouteLocator("/", this.discovery,
				this.properties);
		this.properties.getRoutes().put(ASERVICE, new ZuulRoute("/" + ASERVICE + "/**"));
		this.properties.init();
		Map<String, String> routesMap = routeLocator.getRoutes();
		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertMapping(routesMap, ASERVICE);
	}

	@Test
	public void testGetRoutesWithMapping() {
		ProxyRouteLocator routeLocator = new ProxyRouteLocator("/", this.discovery,
				this.properties);
		this.properties.getRoutes().put(ASERVICE,
				new ZuulRoute("/" + ASERVICE + "/**", ASERVICE));
		this.properties.setPrefix("/foo");

		Map<String, String> routesMap = routeLocator.getRoutes();
		assertMapping(routesMap, ASERVICE, "foo/" + ASERVICE);
	}

	@Test
	public void testGetPhysicalRoutes() {
		ProxyRouteLocator routeLocator = new ProxyRouteLocator("/", this.discovery,
				this.properties);
		this.properties.getRoutes().put(ASERVICE,
				new ZuulRoute("/" + ASERVICE + "/**", "http://" + ASERVICE));
		Map<String, String> routesMap = routeLocator.getRoutes();
		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertMapping(routesMap, "http://" + ASERVICE, ASERVICE);
	}

	@Test
	public void testGetDefaultRoute() {
		ProxyRouteLocator routeLocator = new ProxyRouteLocator("/", this.discovery,
				this.properties);
		this.properties.getRoutes().put(ASERVICE, new ZuulRoute("/**", ASERVICE));
		Map<String, String> routesMap = routeLocator.getRoutes();
		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertDefaultMapping(routesMap, ASERVICE);
	}

	@Test
	public void testGetDefaultPhysicalRoute() {
		ProxyRouteLocator routeLocator = new ProxyRouteLocator("/", this.discovery,
				this.properties);
		this.properties.getRoutes().put(ASERVICE,
				new ZuulRoute("/**", "http://" + ASERVICE));
		Map<String, String> routesMap = routeLocator.getRoutes();
		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertDefaultMapping(routesMap, "http://" + ASERVICE);
	}

	@Test
	public void testIgnoreRoutes() {
		ProxyRouteLocator routeLocator = new ProxyRouteLocator("/", this.discovery,
				this.properties);
		this.properties.setIgnoredServices(Collections.singletonList(IGNOREDSERVICE));
		given(this.discovery.getServices()).willReturn(
				Collections.singletonList(IGNOREDSERVICE));
		Map<String, String> routesMap = routeLocator.getRoutes();
		String serviceId = routesMap.get(getMapping(IGNOREDSERVICE));
		assertNull("routes did not ignore " + IGNOREDSERVICE, serviceId);
	}

	@Test
	public void testIgnoreRoutesWithPattern() {
		ProxyRouteLocator routeLocator = new ProxyRouteLocator("/", this.discovery,
				this.properties);
		this.properties.setIgnoredServices(Collections.singletonList("ignore*"));
		given(this.discovery.getServices()).willReturn(
				Collections.singletonList(IGNOREDSERVICE));
		Map<String, String> routesMap = routeLocator.getRoutes();
		String serviceId = routesMap.get(getMapping(IGNOREDSERVICE));
		assertNull("routes did not ignore " + IGNOREDSERVICE, serviceId);
	}

	@Test
	public void testIgnoreAllRoutes() {
		ProxyRouteLocator routeLocator = new ProxyRouteLocator("/", this.discovery,
				this.properties);
		this.properties.setIgnoredServices(Collections.singletonList("*"));
		given(this.discovery.getServices()).willReturn(
				Collections.singletonList(IGNOREDSERVICE));
		Map<String, String> routesMap = routeLocator.getRoutes();
		String serviceId = routesMap.get(getMapping(IGNOREDSERVICE));
		assertNull("routes did not ignore " + IGNOREDSERVICE, serviceId);
	}

	@Test
	public void testIgnoredRouteIncludedIfConfiguredAndDiscovered() {
		this.properties.getRoutes().put("foo", new ZuulRoute("/foo/**"));
		ProxyRouteLocator routeLocator = new ProxyRouteLocator("/", this.discovery,
				this.properties);
		this.properties.setIgnoredServices(Collections.singletonList("*"));
		given(this.discovery.getServices()).willReturn(Collections.singletonList("foo"));
		Map<String, String> routesMap = routeLocator.getRoutes();
		String serviceId = routesMap.get(getMapping("foo"));
		assertNotNull("routes ignored foo", serviceId);
	}

	@Test
	public void testIgnoredRoutePropertiesRemain() {
		ZuulRoute route = new ZuulRoute("/foo/**");
		route.setStripPrefix(true);
		route.setRetryable(Boolean.TRUE);
		this.properties.getRoutes().put("foo", route);
		ProxyRouteLocator routeLocator = new ProxyRouteLocator("/", this.discovery,
				this.properties);
		this.properties.setIgnoredServices(Collections.singletonList("*"));
		given(this.discovery.getServices()).willReturn(Collections.singletonList("foo"));
		LinkedHashMap<String, ZuulRoute> routes = routeLocator.locateRoutes();
		ZuulRoute actual = routes.get(getMapping("foo"));
		assertNotNull("routes ignored foo", actual);
		assertTrue("stripPrefix is wrong", actual.isStripPrefix());
		assertEquals("retryable is wrong", Boolean.TRUE, actual.getRetryable());
	}

	@Test
	public void testIgnoredRouteNonServiceIdPathRemains() {
		//This is how you setup a route defined like zuul.proxy.route.foo=/**
		ZuulRoute route = new ZuulRoute("/**", "foo");
		route.setId("foo");

		this.properties.getRoutes().put("foo", route);
		ProxyRouteLocator routeLocator = new ProxyRouteLocator("/", this.discovery,
				this.properties);
		this.properties.setIgnoredServices(Collections.singletonList("*"));
		given(this.discovery.getServices()).willReturn(Collections.singletonList("foo"));
		LinkedHashMap<String, ZuulRoute> routes = routeLocator.locateRoutes();
		ZuulRoute actual = routes.get("/**");
		assertNotNull("routes ignored foo", actual);
		assertEquals("id is wrong", "foo", actual.getId());
		assertEquals("location is wrong", "foo", actual.getServiceId());
		assertEquals("path is wrong", "/**", actual.getPath());
	}

	@Test
	public void testIgnoredRouteIncludedIfConfiguredAndNotDiscovered() {
		this.properties.getRoutes()
				.put("foo", new ZuulRoute("/foo/**", "http://www.foo.com/"));
		ProxyRouteLocator routeLocator = new ProxyRouteLocator("/", this.discovery,
				this.properties);
		this.properties.setIgnoredServices(Collections.singletonList("*"));
		given(this.discovery.getServices()).willReturn(Collections.singletonList("bar"));
		Map<String, String> routesMap = routeLocator.getRoutes();
		String id = routesMap.get(getMapping("foo"));
		assertNotNull("routes ignored foo", id);
	}

	@Test
	public void testAutoRoutes() {
		ProxyRouteLocator routeLocator = new ProxyRouteLocator("/", this.discovery,
				this.properties);
		given(this.discovery.getServices()).willReturn(
				Collections.singletonList(MYSERVICE));
		Map<String, String> routesMap = routeLocator.getRoutes();
		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertMapping(routesMap, MYSERVICE);
	}

	@Test
	public void testAutoRoutesCanBeOverridden() {
		ZuulRoute route = new ZuulRoute("/" + MYSERVICE + "/**", "https://example.com/"
				+ MYSERVICE);
		this.properties.getRoutes().put(MYSERVICE, route);
		ProxyRouteLocator routeLocator = new ProxyRouteLocator("/", this.discovery,
				this.properties);
		given(this.discovery.getServices()).willReturn(
				Collections.singletonList(MYSERVICE));
		Map<String, String> routesMap = routeLocator.getRoutes();
		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertMapping(routesMap, "https://example.com/" + MYSERVICE, MYSERVICE);
	}

	protected void assertMapping(Map<String, String> routesMap, String serviceId) {
		assertMapping(routesMap, serviceId, serviceId);
	}

	protected void assertMapping(Map<String, String> routesMap, String expectedRoute,
			String key) {
		String mapping = getMapping(key);
		String route = routesMap.get(mapping);
		assertEquals("routesMap had wrong value for " + mapping, expectedRoute, route);
	}

	private String getMapping(String serviceId) {
		return "/" + serviceId + "/**";
	}

	protected void assertDefaultMapping(Map<String, String> routesMap,
			String expectedRoute) {
		String mapping = "/**";
		String route = routesMap.get(mapping);
		assertEquals("routesMap had wrong value for " + mapping, expectedRoute, route);
	}
}
