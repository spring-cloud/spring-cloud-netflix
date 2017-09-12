/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.zuul.filters.discovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.internal.util.collections.Sets;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;
import org.springframework.cloud.netflix.zuul.route.predicate.MethodRoutePredicateFactory;
import org.springframework.cloud.netflix.zuul.route.predicate.RoutePredicateFactory;
import org.springframework.cloud.netflix.zuul.routematcher.AlternateRouteLookup;
import org.springframework.cloud.netflix.zuul.routematcher.RouteCondition;
import org.springframework.cloud.netflix.zuul.routematcher.RouteOptions;
import org.springframework.cloud.netflix.zuul.util.RequestUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;

import com.netflix.zuul.context.RequestContext;

/**
 * @author Spencer Gibb
 * @author Dave Syer
 */
public class DiscoveryClientRouteMatcherTests {

	public static final String IGNOREDSERVICE = "ignoredservice";

	public static final String IGNOREDPATTERN = "/foo/**";

	public static final String ASERVICE = "aservice";

	public static final String MYSERVICE = "myservice";

	@Mock
	private ConfigurableEnvironment env;

	@Mock
	private DiscoveryClient discovery;

	private ZuulProperties properties = new ZuulProperties();

	public static class RegexMapper {
		private boolean enabled = false;

		private String servicePattern = "(?<name>.*)-(?<version>v.*$)";

		private String routePattern = "${version}/${name}";
		
		public RegexMapper() {
		}

		public RegexMapper(boolean enabled, String servicePattern, String routePattern) {
			this.enabled = enabled;
			this.servicePattern = servicePattern;
			this.routePattern = routePattern;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getServicePattern() {
			return servicePattern;
		}

		public void setServicePattern(String servicePattern) {
			this.servicePattern = servicePattern;
		}

		public String getRoutePattern() {
			return routePattern;
		}

		public void setRoutePattern(String routePattern) {
			this.routePattern = routePattern;
		}
	}

	private RegexMapper regexMapper = new RegexMapper();

	@Before
	public void init() {
		initMocks(this);
		setTestRequestcontext(); // re-initialize Zuul context for each test
	}

	@After
	public void clear() {
		RequestContext.getCurrentContext().clear();
	}

	@Test
	public void testGetMatchingPath() throws Exception {
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		this.properties.getRoutes().put("foo", new ZuulRoute("/foo/**"));
		this.properties.init();
		routeMatcher.getRoutes(); // force refresh
		Route route = routeMatcher
				.getMatchingRoute(createMockRequest("/foo/1"));
		assertEquals("foo", route.getLocation());
		assertEquals("foo", route.getId());
	}

	@Test
	public void testGetMatchingPathWithPrefix() throws Exception {
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		this.properties.getRoutes().put("foo", new ZuulRoute("/foo/**"));
		this.properties.setPrefix("/proxy");
		this.properties.init();
		routeMatcher.getRoutes(); // force refresh
		Route route = routeMatcher
				.getMatchingRoute(createMockRequest("/proxy/foo/1"));
		assertEquals("foo", route.getLocation());
		assertEquals("/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithServletPath() throws Exception {
		setTestRequestcontext();
		RequestContext.getCurrentContext()
				.set(RequestUtils.IS_DISPATCHERSERVLETREQUEST, true);
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/app", this.discovery, this.properties);
		this.properties.getRoutes().put("foo", new ZuulRoute("/foo/**"));
		this.properties.init();
		routeMatcher.getRoutes(); // force refresh
		Route route = routeMatcher
				.getMatchingRoute(createMockRequest("/app/foo/1"));
		assertEquals("foo", route.getLocation());
		assertEquals("/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithZuulServletPath() throws Exception {
		RequestContext.getCurrentContext().setZuulEngineRan();
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/app", this.discovery, this.properties);
		this.properties.getRoutes().put("foo", new ZuulRoute("/foo/**"));
		this.properties.init();
		routeMatcher.getRoutes(); // force refresh
		Route route = routeMatcher
				.getMatchingRoute(createMockRequest("/zuul/foo/1"));
		assertEquals("foo", route.getLocation());
		assertEquals("/1", route.getPath());

	}

	@Test
	public void testGetMatchingPathWithNoPrefixStripping() throws Exception {
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		this.properties.getRoutes().put("foo", new ZuulRoute("foo", "/foo/**",
				"foo", null, false, null, null));
		this.properties.setStripPrefix(false);
		this.properties.setPrefix("/proxy");
		routeMatcher.getRoutes(); // force refresh
		Route route = routeMatcher
				.getMatchingRoute(createMockRequest("/proxy/foo/1"));
		assertEquals("foo", route.getLocation());
		assertEquals("/proxy/foo/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithLocalPrefixStripping() throws Exception {
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		this.properties.getRoutes().put("foo", new ZuulRoute("/foo/**", "foo"));
		this.properties.setStripPrefix(false);
		this.properties.setPrefix("/proxy");
		routeMatcher.getRoutes(); // force refresh
		Route route = routeMatcher
				.getMatchingRoute(createMockRequest("/proxy/foo/1"));
		assertEquals("foo", route.getLocation());
		assertEquals("/proxy/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithGlobalPrefixStripping()
			throws Exception {
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		this.properties.getRoutes().put("foo", new ZuulRoute("foo", "/foo/**",
				"foo", null, false, null, null));
		this.properties.setPrefix("/proxy");
		routeMatcher.getRoutes(); // force refresh
		Route route = routeMatcher
				.getMatchingRoute(createMockRequest("/proxy/foo/1"));
		assertEquals("foo", route.getLocation());
		assertEquals("/foo/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithGlobalPrefixStrippingAndServletPath()
			throws Exception {
		RequestContext.getCurrentContext()
				.set(RequestUtils.IS_DISPATCHERSERVLETREQUEST, true);
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/app", this.discovery, this.properties);
		this.properties.getRoutes().put("foo", new ZuulRoute("foo", "/foo/**",
				"foo", null, false, null, null));
		this.properties.setPrefix("/proxy");
		routeMatcher.getRoutes(); // force refresh
		Route route = routeMatcher
				.getMatchingRoute(createMockRequest("/app/proxy/foo/1"));
		assertEquals("foo", route.getLocation());
		assertEquals("/foo/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithGlobalPrefixStrippingAndZuulServletPath()
			throws Exception {
		RequestContext.getCurrentContext().setZuulEngineRan();
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		this.properties.getRoutes().put("foo", new ZuulRoute("foo", "/foo/**",
				"foo", null, false, null, null));
		this.properties.setPrefix("/proxy");
		routeMatcher.getRoutes(); // force refresh
		Route route = routeMatcher
				.getMatchingRoute(createMockRequest("/zuul/proxy/foo/1"));
		assertEquals("foo", route.getLocation());
		assertEquals("/foo/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithRoutePrefixStripping() throws Exception {
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		ZuulRoute zuulRoute = new ZuulRoute("/foo/**");
		zuulRoute.setStripPrefix(true);
		this.properties.getRoutes().put("foo", zuulRoute);
		this.properties.init();
		routeMatcher.getRoutes(); // force refresh
		Route route = routeMatcher
				.getMatchingRoute(createMockRequest("/foo/1"));
		assertEquals("foo", route.getLocation());
		assertEquals("/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithoutMatchingIgnoredPattern()
			throws Exception {
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		this.properties
				.setIgnoredPatterns(Collections.singleton(IGNOREDPATTERN));
		this.properties.getRoutes().put("bar", new ZuulRoute("/bar/**"));
		this.properties.init();
		routeMatcher.getRoutes(); // force refresh
		Route route = routeMatcher
				.getMatchingRoute(createMockRequest("/bar/1"));
		assertEquals("bar", route.getLocation());
		assertEquals("bar", route.getId());
	}

	@Test
	public void testGetMatchingPathWithMatchingIgnoredPattern()
			throws Exception {
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		this.properties
				.setIgnoredPatterns(Collections.singleton(IGNOREDPATTERN));
		this.properties.getRoutes().put("foo", new ZuulRoute("/foo/**"));
		this.properties.init();
		routeMatcher.getRoutes(); // force refresh
		Route route = routeMatcher
				.getMatchingRoute(createMockRequest("/foo/1"));
		assertNull("routes did not ignore " + IGNOREDPATTERN, route);
	}

	@Test
	public void testGetMatchingPathWithMatchingIgnoredPatternWithPrefix()
			throws Exception {
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		this.properties
				.setIgnoredPatterns(Collections.singleton(IGNOREDPATTERN));
		this.properties.getRoutes().put("foo", new ZuulRoute("/foo/**"));
		this.properties.setPrefix("/proxy");
		this.properties.init();
		routeMatcher.getRoutes(); // force refresh
		Route route = routeMatcher
				.getMatchingRoute(createMockRequest("/proxy/foo/1"));
		assertEquals("foo", route.getLocation());
		assertEquals("/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithMatchingIgnoredPatternWithServletPath()
			throws Exception {
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/app", this.discovery, this.properties);
		this.properties
				.setIgnoredPatterns(Collections.singleton(IGNOREDPATTERN));
		this.properties.getRoutes().put("foo", new ZuulRoute("/foo/**"));
		this.properties.init();
		routeMatcher.getRoutes(); // force refresh
		Route route = routeMatcher
				.getMatchingRoute(createMockRequest("/app/foo/1"));
		assertNull("routes did not ignore " + IGNOREDPATTERN, route);
	}

	@Test
	public void testGetMatchingPathWithoutMatchingIgnoredPatternWithNoPrefixStripping()
			throws Exception {
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		this.properties
				.setIgnoredPatterns(Collections.singleton(IGNOREDPATTERN));
		this.properties.getRoutes().put("foo", new ZuulRoute("foo", "/foo/**",
				"foo", null, false, null, null));
		this.properties.setStripPrefix(false);
		this.properties.setPrefix("/proxy");
		routeMatcher.getRoutes(); // force refresh
		Route route = routeMatcher
				.getMatchingRoute(createMockRequest("/proxy/foo/1"));
		assertEquals("foo", route.getLocation());
		assertEquals("/proxy/foo/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithMatchingIgnoredPatternWithNoPrefixStripping()
			throws Exception {
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		this.properties.setIgnoredPatterns(
				Collections.singleton("/proxy" + IGNOREDPATTERN));
		this.properties.getRoutes().put("foo", new ZuulRoute("foo", "/foo/**",
				"foo", null, false, null, null));
		this.properties.setStripPrefix(false);
		this.properties.setPrefix("/proxy");
		routeMatcher.getRoutes(); // force refresh
		Route route = routeMatcher
				.getMatchingRoute(createMockRequest("/proxy/foo/1"));
		assertNull("routes did not ignore " + "/proxy" + IGNOREDPATTERN, route);
	}

	@Test
	public void testGetMatchingPathWithoutMatchingIgnoredPatternWithLocalPrefixStripping()
			throws Exception {
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		this.properties
				.setIgnoredPatterns(Collections.singleton(IGNOREDPATTERN));
		this.properties.getRoutes().put("foo", new ZuulRoute("/foo/**", "foo"));
		this.properties.setStripPrefix(false);
		this.properties.setPrefix("/proxy");
		routeMatcher.getRoutes(); // force refresh
		Route route = routeMatcher
				.getMatchingRoute(createMockRequest("/proxy/foo/1"));
		assertEquals("foo", route.getLocation());
		assertEquals("/proxy/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithMatchingIgnoredPatternWithLocalPrefixStripping()
			throws Exception {
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		this.properties.setIgnoredPatterns(
				Collections.singleton("/proxy" + IGNOREDPATTERN));
		this.properties.getRoutes().put("foo", new ZuulRoute("/foo/**", "foo"));
		this.properties.setStripPrefix(false);
		this.properties.setPrefix("/proxy");
		routeMatcher.getRoutes(); // force refresh
		Route route = routeMatcher
				.getMatchingRoute(createMockRequest("/proxy/foo/1"));
		assertNull("routes did not ignore " + "/proxy" + IGNOREDPATTERN, route);
	}

	@Test
	public void testGetMatchingPathWithoutMatchingIgnoredPatternWithGlobalPrefixStripping()
			throws Exception {
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		this.properties
				.setIgnoredPatterns(Collections.singleton(IGNOREDPATTERN));
		this.properties.getRoutes().put("foo", new ZuulRoute("foo", "/foo/**",
				"foo", null, false, null, null));
		this.properties.setPrefix("/proxy");
		routeMatcher.getRoutes(); // force refresh
		Route route = routeMatcher
				.getMatchingRoute(createMockRequest("/proxy/foo/1"));
		assertEquals("foo", route.getLocation());
		assertEquals("/foo/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithMatchingIgnoredPatternWithGlobalPrefixStripping()
			throws Exception {
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		this.properties.setIgnoredPatterns(
				Collections.singleton("/proxy" + IGNOREDPATTERN));
		this.properties.getRoutes().put("foo", new ZuulRoute("foo", "/foo/**",
				"foo", null, false, null, null));
		this.properties.setPrefix("/proxy");
		routeMatcher.getRoutes(); // force refresh
		Route route = routeMatcher
				.getMatchingRoute(createMockRequest("/proxy/foo/1"));
		assertNull("routes did not ignore " + "/proxy" + IGNOREDPATTERN, route);
	}

	@Test
	public void testGetMatchingPathWithMatchingIgnoredPatternWithRoutePrefixStripping()
			throws Exception {
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		ZuulRoute zuulRoute = new ZuulRoute("/foo/**");
		zuulRoute.setStripPrefix(true);
		this.properties
				.setIgnoredPatterns(Collections.singleton(IGNOREDPATTERN));
		this.properties.getRoutes().put("foo", zuulRoute);
		this.properties.init();
		routeMatcher.getRoutes(); // force refresh
		Route route = routeMatcher
				.getMatchingRoute(createMockRequest("/foo/1"));
		assertNull("routes did not ignore " + IGNOREDPATTERN, route);
	}

	@Test
	public void testGetRoutes() {
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		this.properties.getRoutes().put(ASERVICE,
				new ZuulRoute("/" + ASERVICE + "/**"));
		this.properties.init();
		List<Route> routesMap = routeMatcher.getRoutes();
		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertMapping(routesMap, ASERVICE);
	}

	@Test
	public void testGetRoutesWithMapping() {
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		this.properties.getRoutes().put(ASERVICE,
				new ZuulRoute("/" + ASERVICE + "/**", ASERVICE));
		this.properties.setPrefix("/foo");

		List<Route> routesMap = routeMatcher.getRoutes();
		assertMapping(routesMap, ASERVICE, "foo/" + ASERVICE);
	}

	@Test
	public void testGetPhysicalRoutes() {
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		this.properties.getRoutes().put(ASERVICE,
				new ZuulRoute("/" + ASERVICE + "/**", "http://" + ASERVICE));
		List<Route> routesMap = routeMatcher.getRoutes();
		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertMapping(routesMap, "http://" + ASERVICE, ASERVICE);
	}

	@Test
	public void testGetDefaultRoute() {
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		this.properties.getRoutes().put(ASERVICE,
				new ZuulRoute("/**", ASERVICE));
		List<Route> routesMap = routeMatcher.getRoutes();
		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertDefaultMapping(routesMap, ASERVICE);
	}

	@Test
	public void testGetDefaultPhysicalRoute() {
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		this.properties.getRoutes().put(ASERVICE,
				new ZuulRoute("/**", "http://" + ASERVICE));
		List<Route> routesMap = routeMatcher.getRoutes();
		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertDefaultMapping(routesMap, "http://" + ASERVICE);
	}

	@Test
	public void testIgnoreRoutes() {
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		this.properties
				.setIgnoredServices(Collections.singleton(IGNOREDSERVICE));
		given(this.discovery.getServices())
				.willReturn(Collections.singletonList(IGNOREDSERVICE));
		List<Route> routesMap = routeMatcher.getRoutes();
		assertNull("routes did not ignore " + IGNOREDSERVICE,
				getRoute(routesMap, getMapping(IGNOREDSERVICE)));
	}

	@Test
	public void testIgnoreRoutesWithPattern() {
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		this.properties.setIgnoredServices(Collections.singleton("ignore*"));
		given(this.discovery.getServices())
				.willReturn(Collections.singletonList(IGNOREDSERVICE));
		List<Route> routesMap = routeMatcher.getRoutes();
		assertNull("routes did not ignore " + IGNOREDSERVICE,
				getRoute(routesMap, getMapping(IGNOREDSERVICE)));
	}

	@Test
	public void testIgnoreAllRoutes() {
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		this.properties.setIgnoredServices(Collections.singleton("*"));
		given(this.discovery.getServices())
				.willReturn(Collections.singletonList(IGNOREDSERVICE));
		List<Route> routesMap = routeMatcher.getRoutes();
		assertNull("routes did not ignore " + IGNOREDSERVICE,
				getRoute(routesMap, getMapping(IGNOREDSERVICE)));
	}

	@Test
	public void testIgnoredRouteIncludedIfConfiguredAndDiscovered() {
		this.properties.getRoutes().put("foo", new ZuulRoute("/foo/**"));
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		this.properties.setIgnoredServices(Collections.singleton("*"));
		given(this.discovery.getServices())
				.willReturn(Collections.singletonList("foo"));
		List<Route> routesMap = routeMatcher.getRoutes();
		assertNotNull("routes ignored foo", getRoute(routesMap, "/foo/**"));
	}

	@Test
	public void testIgnoredRoutePropertiesRemain() {
		ZuulRoute route = new ZuulRoute("/foo/**");
		route.setStripPrefix(true);
		route.setRetryable(Boolean.TRUE);
		this.properties.getRoutes().put("foo", route);
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		this.properties.setIgnoredServices(Collections.singleton("*"));
		given(this.discovery.getServices())
				.willReturn(Collections.singletonList("foo"));
		LinkedHashMap<String, ZuulRoute> routes = routeMatcher.locateRoutes();
		ZuulRoute actual = routes.get("/foo/**");
		assertNotNull("routes ignored foo", actual);
		assertTrue("stripPrefix is wrong", actual.isStripPrefix());
		assertEquals("retryable is wrong", Boolean.TRUE, actual.getRetryable());
	}

	@Test
	public void testIgnoredRouteNonServiceIdPathRemains() {
		// This is how you setup a route defined like zuul.proxy.route.foo=/**
		ZuulRoute route = new ZuulRoute("/**", "foo");
		route.setId("foo");

		this.properties.getRoutes().put("foo", route);
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		this.properties.setIgnoredServices(Collections.singleton("*"));
		given(this.discovery.getServices())
				.willReturn(Collections.singletonList("foo"));
		LinkedHashMap<String, ZuulRoute> routes = routeMatcher.locateRoutes();
		ZuulRoute actual = routes.get("/**");
		assertNotNull("routes ignored foo", actual);
		assertEquals("id is wrong", "foo", actual.getId());
		assertEquals("location is wrong", "foo", actual.getServiceId());
		assertEquals("path is wrong", "/**", actual.getPath());
	}

	@Test
	public void testIgnoredRouteIncludedIfConfiguredAndNotDiscovered() {
		this.properties.getRoutes().put("foo",
				new ZuulRoute("/foo/**", "http://foo.com"));
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		this.properties.setIgnoredServices(Collections.singleton("*"));
		given(this.discovery.getServices())
				.willReturn(Collections.singletonList("bar"));
		List<Route> routesMap = routeMatcher.getRoutes();
		assertNotNull("routes ignored foo",
				getRoute(routesMap, getMapping("foo")));
	}

	@Test
	public void testAutoRoutes() {
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		given(this.discovery.getServices())
				.willReturn(Collections.singletonList(MYSERVICE));
		List<Route> routesMap = routeMatcher.getRoutes();
		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertMapping(routesMap, MYSERVICE);
	}

	@Test
	public void testAutoRoutesCanBeOverridden() {
		ZuulRoute route = new ZuulRoute("/" + MYSERVICE + "/**",
				"http://example.com/" + MYSERVICE);
		this.properties.getRoutes().put(MYSERVICE, route);
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		given(this.discovery.getServices())
				.willReturn(Collections.singletonList(MYSERVICE));
		List<Route> routesMap = routeMatcher.getRoutes();
		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertMapping(routesMap, "http://example.com/" + MYSERVICE, MYSERVICE);
	}

	@Test
	public void testIgnoredLocalServiceByDefault() {
		given(this.discovery.getServices())
				.willReturn(Collections.singletonList(MYSERVICE));
		given(this.discovery.getLocalServiceInstance()).willReturn(
				new DefaultServiceInstance(MYSERVICE, "localhost", 80, false));

		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);

		LinkedHashMap<String, ZuulRoute> routes = routeMatcher.locateRoutes();
		ZuulRoute actual = routes.get("/**");
		assertNull("routes didn't ignore " + MYSERVICE, actual);

		List<Route> routesMap = routeMatcher.getRoutes();
		assertNotNull("routesMap was null", routesMap);
		assertTrue("routesMap was empty", routesMap.isEmpty());
	}

	@Test
	public void testIgnoredLocalServiceFalse() {
		this.properties.setIgnoreLocalService(false);

		given(this.discovery.getServices())
				.willReturn(Collections.singletonList(MYSERVICE));

		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);

		List<Route> routesMap = routeMatcher.getRoutes();
		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertMapping(routesMap, MYSERVICE);
	}

	@Test
	public void testLocalServiceExceptionIgnored() {
		given(this.discovery.getServices())
				.willReturn(Collections.<String> emptyList());
		given(this.discovery.getLocalServiceInstance())
				.willThrow(new RuntimeException());

		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);

		// if no exception is thrown in constructor, this is a success
		routeMatcher.locateRoutes();
	}

	@Test
	public void testRegExServiceRouteMapperNoServiceIdMatches() {
		given(this.discovery.getServices())
				.willReturn(Collections.singletonList(MYSERVICE));

		PatternServiceRouteMapper regExServiceRouteMapper = new PatternServiceRouteMapper(
				this.regexMapper.getServicePattern(),
				this.regexMapper.getRoutePattern());
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties, regExServiceRouteMapper);
		List<Route> routesMap = routeMatcher.getRoutes();
		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertMapping(routesMap, MYSERVICE);
	}

	@Test
	public void testRegExServiceRouteMapperServiceIdMatches() {
		given(this.discovery.getServices())
				.willReturn(Collections.singletonList("rest-service-v1"));

		PatternServiceRouteMapper regExServiceRouteMapper = new PatternServiceRouteMapper(
				this.regexMapper.getServicePattern(),
				this.regexMapper.getRoutePattern());
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties, regExServiceRouteMapper);
		List<Route> routesMap = routeMatcher.getRoutes();
		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertMapping(routesMap, "rest-service-v1", "v1/rest-service");
	}

	@Test
	public void testMatchGetIfRouteHasNoMethodSpecified() throws Exception {
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		this.properties.getRoutes().put("foo", new ZuulRoute("foo", "/foo/**",
				"foo", null, false, null, null));
		this.properties.setPrefix("/proxy");
		routeMatcher.getRoutes(); // force refresh
		Route route = routeMatcher
				.getMatchingRoute(createMockRequest("GET", "/proxy/foo/1"));
		assertEquals("foo", route.getLocation());
		assertEquals("/foo/1", route.getPath());
	}

	@Test
	public void testMatchGetIfRouteHasOnlyGetSpecified() throws Exception {
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		this.properties.getRoutes().put("foo", new ZuulRoute("foo", "/foo/**",
				"foo", null, false, null, null));
		this.properties.setPrefix("/proxy");
		routeMatcher.getRoutes(); // force refresh
		Route route = routeMatcher
				.getMatchingRoute(createMockRequest("GET", "/proxy/foo/1"));
		assertEquals("foo", route.getLocation());
		assertEquals("/foo/1", route.getPath());
	}

	@Test
	public void testMatchGETIfRouteHasGetAndPostSpecified() throws Exception {
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);
		this.properties.getRoutes().put("foo", new ZuulRoute("foo", "/foo/**",
				"foo", null, false, null, null));
		this.properties.setPrefix("/proxy");
		routeMatcher.getRoutes(); // force refresh
		Route route = routeMatcher
				.getMatchingRoute(createMockRequest("GET", "/proxy/foo/1"));
		assertEquals("foo", route.getLocation());
		assertEquals("/foo/1", route.getPath());
	}

	@Test
	public void testNotMatchGetIfRouteHasOnlyPostSpecified() throws Exception {
		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties);

		RouteOptions routeOptions = new RouteOptions();
		routeOptions.setAllowedMethods(Sets.newSet("POST"));

		this.properties.getRoutes().put("foo", new ZuulRoute("foo", "/foo/**",
				"foo", null, false, null, null, routeOptions));
		this.properties.setPrefix("/proxy");
		routeMatcher.getRoutes(); // force refresh
		Route route = routeMatcher
				.getMatchingRoute(createMockRequest("GET", "/proxy/foo/1"));
		assertNull(route);
	}

	@Test
	public void testMatchDifferentRouteForGetAndPost() throws Exception {

		List<RoutePredicateFactory> predicates = new ArrayList<RoutePredicateFactory>();
		predicates.add(new MethodRoutePredicateFactory());

		DiscoveryClientRouteMatcher routeMatcher = new DiscoveryClientRouteMatcher(
				"/", this.discovery, this.properties, new AlternateRouteLookup(predicates));

		RouteOptions routeOptions = new RouteOptions();
		routeOptions.setAllowedMethods(Sets.newSet("GET", "POST"));
		routeOptions.getRouteConditions().add(new RouteCondition("get-operation=location-get,Method=GET"));
		routeOptions.getRouteConditions().add(new RouteCondition("post-operation=location-post,Method=POST"));

		this.properties.getRoutes().put("foo", new ZuulRoute("foo", "/foo/**",
				"foo", null, false, null, null, routeOptions));

		this.properties.setPrefix("/proxy");
		routeMatcher.getRoutes(); // force refresh
		Route route = routeMatcher.getMatchingRoute(
				createMockRequest("GET", "/proxy/foo/service"));
		assertEquals("location-get", route.getLocation());
		assertEquals("/foo/service", route.getPath());

		route = routeMatcher.getMatchingRoute(
				createMockRequest("POST", "/proxy/foo/service"));
		assertEquals("location-post", route.getLocation());
		assertEquals("/foo/service", route.getPath());
	}

	protected void assertMapping(List<Route> routesMap, String serviceId) {
		assertMapping(routesMap, serviceId, serviceId);
	}

	protected void assertMapping(List<Route> routesMap, String expectedRoute,
			String key) {
		String mapping = getMapping(key);
		Route route = getRoute(routesMap, mapping);
		assertNotNull("Could not find route for " + key, route);
		String location = route.getLocation();
		assertEquals("routesMap had wrong value for " + mapping, expectedRoute,
				location);
	}

	private String getMapping(String serviceId) {
		return "/" + serviceId + "/**";
	}

	protected void assertDefaultMapping(List<Route> routesMap,
			String expectedRoute) {
		String mapping = "/**";
		String route = getRoute(routesMap, mapping).getLocation();
		assertEquals("routesMap had wrong value for " + mapping, expectedRoute,
				route);
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

	private void setTestRequestcontext() {
		RequestContext context = new RequestContext();
		RequestContext.testSetCurrentContext(context);
	}

	private HttpServletRequest createMockRequest(String path) {
		return new MockHttpServletRequest(null, path);
	}

	private HttpServletRequest createMockRequest(String method, String path) {
		return new MockHttpServletRequest(method, path);

	}
}
