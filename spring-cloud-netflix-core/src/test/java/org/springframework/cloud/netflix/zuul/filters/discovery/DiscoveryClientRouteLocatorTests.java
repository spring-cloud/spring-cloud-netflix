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

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;
import org.springframework.cloud.netflix.zuul.util.RequestUtils;
import org.springframework.core.env.ConfigurableEnvironment;

import com.netflix.zuul.context.RequestContext;

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
public class DiscoveryClientRouteLocatorTests {

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
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		this.properties.getRoutes().put("foo", new ZuulRoute("/foo/**"));
		this.properties.init();
		routeLocator.getRoutes(); // force refresh
		Route route = routeLocator.getMatchingRoute("/foo/1");
		assertEquals("foo", route.getLocation());
		assertEquals("foo", route.getId());
	}

	@Test
	public void testGetMatchingPathWithPrefix() throws Exception {
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		this.properties.getRoutes().put("foo", new ZuulRoute("/foo/**"));
		this.properties.setPrefix("/proxy");
		this.properties.init();
		routeLocator.getRoutes(); // force refresh
		Route route = routeLocator.getMatchingRoute("/proxy/foo/1");
		assertEquals("foo", route.getLocation());
		assertEquals("/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithServletPath() throws Exception {
		setTestRequestcontext();
		RequestContext.getCurrentContext().set(RequestUtils.IS_DISPATCHERSERVLETREQUEST,
				true);
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/app",
				this.discovery, this.properties);
		this.properties.getRoutes().put("foo", new ZuulRoute("/foo/**"));
		this.properties.init();
		routeLocator.getRoutes(); // force refresh
		Route route = routeLocator.getMatchingRoute("/app/foo/1");
		assertEquals("foo", route.getLocation());
		assertEquals("/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithZuulServletPath() throws Exception {
		RequestContext.getCurrentContext().setZuulEngineRan();
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/app",
				this.discovery, this.properties);
		this.properties.getRoutes().put("foo", new ZuulRoute("/foo/**"));
		this.properties.init();
		routeLocator.getRoutes(); // force refresh
		Route route = routeLocator.getMatchingRoute("/zuul/foo/1");
		assertEquals("foo", route.getLocation());
		assertEquals("/1", route.getPath());

	}

	@Test
	public void testGetMatchingPathWithNoPrefixStripping() throws Exception {
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		this.properties.getRoutes().put("foo",
				new ZuulRoute("foo", "/foo/**", "foo", null, false, null, null));
		this.properties.setStripPrefix(false);
		this.properties.setPrefix("/proxy");
		routeLocator.getRoutes(); // force refresh
		Route route = routeLocator.getMatchingRoute("/proxy/foo/1");
		assertEquals("foo", route.getLocation());
		assertEquals("/proxy/foo/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithLocalPrefixStripping() throws Exception {
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		this.properties.getRoutes().put("foo", new ZuulRoute("/foo/**", "foo"));
		this.properties.setStripPrefix(false);
		this.properties.setPrefix("/proxy");
		routeLocator.getRoutes(); // force refresh
		Route route = routeLocator.getMatchingRoute("/proxy/foo/1");
		assertEquals("foo", route.getLocation());
		assertEquals("/proxy/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithGlobalPrefixStripping() throws Exception {
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		this.properties.getRoutes().put("foo",
				new ZuulRoute("foo", "/foo/**", "foo", null, false, null, null));
		this.properties.setPrefix("/proxy");
		routeLocator.getRoutes(); // force refresh
		Route route = routeLocator.getMatchingRoute("/proxy/foo/1");
		assertEquals("foo", route.getLocation());
		assertEquals("/foo/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithGlobalPrefixStrippingAndServletPath()
			throws Exception {
		RequestContext.getCurrentContext().set(RequestUtils.IS_DISPATCHERSERVLETREQUEST,
				true);
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/app",
				this.discovery, this.properties);
		this.properties.getRoutes().put("foo",
				new ZuulRoute("foo", "/foo/**", "foo", null, false, null, null));
		this.properties.setPrefix("/proxy");
		routeLocator.getRoutes(); // force refresh
		Route route = routeLocator.getMatchingRoute("/app/proxy/foo/1");
		assertEquals("foo", route.getLocation());
		assertEquals("/foo/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithGlobalPrefixStrippingAndZuulServletPath()
			throws Exception {
		RequestContext.getCurrentContext().setZuulEngineRan();
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		this.properties.getRoutes().put("foo",
				new ZuulRoute("foo", "/foo/**", "foo", null, false, null, null));
		this.properties.setPrefix("/proxy");
		routeLocator.getRoutes(); // force refresh
		Route route = routeLocator.getMatchingRoute("/zuul/proxy/foo/1");
		assertEquals("foo", route.getLocation());
		assertEquals("/foo/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithRoutePrefixStripping() throws Exception {
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		ZuulRoute zuulRoute = new ZuulRoute("/foo/**");
		zuulRoute.setStripPrefix(true);
		this.properties.getRoutes().put("foo", zuulRoute);
		this.properties.init();
		routeLocator.getRoutes(); // force refresh
		Route route = routeLocator.getMatchingRoute("/foo/1");
		assertEquals("foo", route.getLocation());
		assertEquals("/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithoutMatchingIgnoredPattern() throws Exception {
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		this.properties.setIgnoredPatterns(Collections.singleton(IGNOREDPATTERN));
		this.properties.getRoutes().put("bar", new ZuulRoute("/bar/**"));
		this.properties.init();
		routeLocator.getRoutes(); // force refresh
		Route route = routeLocator.getMatchingRoute("/bar/1");
		assertEquals("bar", route.getLocation());
		assertEquals("bar", route.getId());
	}

	@Test
	public void testGetMatchingPathWithMatchingIgnoredPattern() throws Exception {
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		this.properties.setIgnoredPatterns(Collections.singleton(IGNOREDPATTERN));
		this.properties.getRoutes().put("foo", new ZuulRoute("/foo/**"));
		this.properties.init();
		routeLocator.getRoutes(); // force refresh
		Route route = routeLocator.getMatchingRoute("/foo/1");
		assertNull("routes did not ignore " + IGNOREDPATTERN, route);
	}

	@Test
	public void testGetMatchingPathWithMatchingIgnoredPatternWithPrefix()
			throws Exception {
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		this.properties.setIgnoredPatterns(Collections.singleton(IGNOREDPATTERN));
		this.properties.getRoutes().put("foo", new ZuulRoute("/foo/**"));
		this.properties.setPrefix("/proxy");
		this.properties.init();
		routeLocator.getRoutes(); // force refresh
		Route route = routeLocator.getMatchingRoute("/proxy/foo/1");
		assertEquals("foo", route.getLocation());
		assertEquals("/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithMatchingIgnoredPatternWithServletPath()
			throws Exception {
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/app",
				this.discovery, this.properties);
		this.properties.setIgnoredPatterns(Collections.singleton(IGNOREDPATTERN));
		this.properties.getRoutes().put("foo", new ZuulRoute("/foo/**"));
		this.properties.init();
		routeLocator.getRoutes(); // force refresh
		Route route = routeLocator.getMatchingRoute("/app/foo/1");
		assertNull("routes did not ignore " + IGNOREDPATTERN, route);
	}

	@Test
	public void testGetMatchingPathWithoutMatchingIgnoredPatternWithNoPrefixStripping()
			throws Exception {
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		this.properties.setIgnoredPatterns(Collections.singleton(IGNOREDPATTERN));
		this.properties.getRoutes().put("foo",
				new ZuulRoute("foo", "/foo/**", "foo", null, false, null, null));
		this.properties.setStripPrefix(false);
		this.properties.setPrefix("/proxy");
		routeLocator.getRoutes(); // force refresh
		Route route = routeLocator.getMatchingRoute("/proxy/foo/1");
		assertEquals("foo", route.getLocation());
		assertEquals("/proxy/foo/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithMatchingIgnoredPatternWithNoPrefixStripping()
			throws Exception {
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		this.properties
				.setIgnoredPatterns(Collections.singleton("/proxy" + IGNOREDPATTERN));
		this.properties.getRoutes().put("foo",
				new ZuulRoute("foo", "/foo/**", "foo", null, false, null, null));
		this.properties.setStripPrefix(false);
		this.properties.setPrefix("/proxy");
		routeLocator.getRoutes(); // force refresh
		Route route = routeLocator.getMatchingRoute("/proxy/foo/1");
		assertNull("routes did not ignore " + "/proxy" + IGNOREDPATTERN, route);
	}

	@Test
	public void testGetMatchingPathWithoutMatchingIgnoredPatternWithLocalPrefixStripping()
			throws Exception {
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		this.properties.setIgnoredPatterns(Collections.singleton(IGNOREDPATTERN));
		this.properties.getRoutes().put("foo", new ZuulRoute("/foo/**", "foo"));
		this.properties.setStripPrefix(false);
		this.properties.setPrefix("/proxy");
		routeLocator.getRoutes(); // force refresh
		Route route = routeLocator.getMatchingRoute("/proxy/foo/1");
		assertEquals("foo", route.getLocation());
		assertEquals("/proxy/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithMatchingIgnoredPatternWithLocalPrefixStripping()
			throws Exception {
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		this.properties
				.setIgnoredPatterns(Collections.singleton("/proxy" + IGNOREDPATTERN));
		this.properties.getRoutes().put("foo", new ZuulRoute("/foo/**", "foo"));
		this.properties.setStripPrefix(false);
		this.properties.setPrefix("/proxy");
		routeLocator.getRoutes(); // force refresh
		Route route = routeLocator.getMatchingRoute("/proxy/foo/1");
		assertNull("routes did not ignore " + "/proxy" + IGNOREDPATTERN, route);
	}

	@Test
	public void testGetMatchingPathWithoutMatchingIgnoredPatternWithGlobalPrefixStripping()
			throws Exception {
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		this.properties.setIgnoredPatterns(Collections.singleton(IGNOREDPATTERN));
		this.properties.getRoutes().put("foo",
				new ZuulRoute("foo", "/foo/**", "foo", null, false, null, null));
		this.properties.setPrefix("/proxy");
		routeLocator.getRoutes(); // force refresh
		Route route = routeLocator.getMatchingRoute("/proxy/foo/1");
		assertEquals("foo", route.getLocation());
		assertEquals("/foo/1", route.getPath());
	}

	@Test
	public void testGetMatchingPathWithMatchingIgnoredPatternWithGlobalPrefixStripping()
			throws Exception {
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		this.properties
				.setIgnoredPatterns(Collections.singleton("/proxy" + IGNOREDPATTERN));
		this.properties.getRoutes().put("foo",
				new ZuulRoute("foo", "/foo/**", "foo", null, false, null, null));
		this.properties.setPrefix("/proxy");
		routeLocator.getRoutes(); // force refresh
		Route route = routeLocator.getMatchingRoute("/proxy/foo/1");
		assertNull("routes did not ignore " + "/proxy" + IGNOREDPATTERN, route);
	}

	@Test
	public void testGetMatchingPathWithMatchingIgnoredPatternWithRoutePrefixStripping()
			throws Exception {
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		ZuulRoute zuulRoute = new ZuulRoute("/foo/**");
		zuulRoute.setStripPrefix(true);
		this.properties.setIgnoredPatterns(Collections.singleton(IGNOREDPATTERN));
		this.properties.getRoutes().put("foo", zuulRoute);
		this.properties.init();
		routeLocator.getRoutes(); // force refresh
		Route route = routeLocator.getMatchingRoute("/foo/1");
		assertNull("routes did not ignore " + IGNOREDPATTERN, route);
	}

	@Test
	public void testGetRoutes() {
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		this.properties.getRoutes().put(ASERVICE, new ZuulRoute("/" + ASERVICE + "/**"));
		this.properties.init();
		List<Route> routesMap = routeLocator.getRoutes();
		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertMapping(routesMap, ASERVICE);
	}

	@Test
	public void testGetRoutesWithMapping() {
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		this.properties.getRoutes().put(ASERVICE,
				new ZuulRoute("/" + ASERVICE + "/**", ASERVICE));
		this.properties.setPrefix("/foo");

		List<Route> routesMap = routeLocator.getRoutes();
		assertMapping(routesMap, ASERVICE, "foo/" + ASERVICE);
	}

	@Test
	public void testGetPhysicalRoutes() {
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		this.properties.getRoutes().put(ASERVICE,
				new ZuulRoute("/" + ASERVICE + "/**", "http://" + ASERVICE));
		List<Route> routesMap = routeLocator.getRoutes();
		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertMapping(routesMap, "http://" + ASERVICE, ASERVICE);
	}

	@Test
	public void testGetDefaultRoute() {
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		this.properties.getRoutes().put(ASERVICE, new ZuulRoute("/**", ASERVICE));
		List<Route> routesMap = routeLocator.getRoutes();
		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertDefaultMapping(routesMap, ASERVICE);
	}

	@Test
	public void testGetDefaultPhysicalRoute() {
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		this.properties.getRoutes().put(ASERVICE,
				new ZuulRoute("/**", "http://" + ASERVICE));
		List<Route> routesMap = routeLocator.getRoutes();
		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertDefaultMapping(routesMap, "http://" + ASERVICE);
	}

	@Test
	public void testIgnoreRoutes() {
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		this.properties.setIgnoredServices(Collections.singleton(IGNOREDSERVICE));
		given(this.discovery.getServices())
				.willReturn(Collections.singletonList(IGNOREDSERVICE));
		List<Route> routesMap = routeLocator.getRoutes();
		assertNull("routes did not ignore " + IGNOREDSERVICE,
				getRoute(routesMap, getMapping(IGNOREDSERVICE)));
	}

	@Test
	public void testIgnoreRoutesWithPattern() {
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		this.properties.setIgnoredServices(Collections.singleton("ignore*"));
		given(this.discovery.getServices())
				.willReturn(Collections.singletonList(IGNOREDSERVICE));
		List<Route> routesMap = routeLocator.getRoutes();
		assertNull("routes did not ignore " + IGNOREDSERVICE,
				getRoute(routesMap, getMapping(IGNOREDSERVICE)));
	}

	@Test
	public void testIgnoreAllRoutes() {
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		this.properties.setIgnoredServices(Collections.singleton("*"));
		given(this.discovery.getServices())
				.willReturn(Collections.singletonList(IGNOREDSERVICE));
		List<Route> routesMap = routeLocator.getRoutes();
		assertNull("routes did not ignore " + IGNOREDSERVICE,
				getRoute(routesMap, getMapping(IGNOREDSERVICE)));
	}

	@Test
	public void testIgnoredRouteIncludedIfConfiguredAndDiscovered() {
		this.properties.getRoutes().put("foo", new ZuulRoute("/foo/**"));
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		this.properties.setIgnoredServices(Collections.singleton("*"));
		given(this.discovery.getServices()).willReturn(Collections.singletonList("foo"));
		List<Route> routesMap = routeLocator.getRoutes();
		assertNotNull("routes ignored foo", getRoute(routesMap, "/foo/**"));
	}

	@Test
	public void testIgnoredRoutePropertiesRemain() {
		ZuulRoute route = new ZuulRoute("/foo/**");
		route.setStripPrefix(true);
		route.setRetryable(Boolean.TRUE);
		this.properties.getRoutes().put("foo", route);
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		this.properties.setIgnoredServices(Collections.singleton("*"));
		given(this.discovery.getServices()).willReturn(Collections.singletonList("foo"));
		LinkedHashMap<String, ZuulRoute> routes = routeLocator.locateRoutes();
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
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		this.properties.setIgnoredServices(Collections.singleton("*"));
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
		this.properties.getRoutes().put("foo",
				new ZuulRoute("/foo/**", "http://foo.com"));
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		this.properties.setIgnoredServices(Collections.singleton("*"));
		given(this.discovery.getServices()).willReturn(Collections.singletonList("bar"));
		List<Route> routesMap = routeLocator.getRoutes();
		assertNotNull("routes ignored foo", getRoute(routesMap, getMapping("foo")));
	}

	@Test
	public void testAutoRoutes() {
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		given(this.discovery.getServices())
				.willReturn(Collections.singletonList(MYSERVICE));
		List<Route> routesMap = routeLocator.getRoutes();
		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertMapping(routesMap, MYSERVICE);
	}

	@Test
	public void testAutoRoutesCanBeOverridden() {
		ZuulRoute route = new ZuulRoute("/" + MYSERVICE + "/**",
				"http://example.com/" + MYSERVICE);
		this.properties.getRoutes().put(MYSERVICE, route);
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);
		given(this.discovery.getServices())
				.willReturn(Collections.singletonList(MYSERVICE));
		List<Route> routesMap = routeLocator.getRoutes();
		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertMapping(routesMap, "http://example.com/" + MYSERVICE, MYSERVICE);
	}

	@Test
	public void testIgnoredLocalServiceByDefault() {
		given(this.discovery.getServices())
				.willReturn(Collections.singletonList(MYSERVICE));
		Registration registration = new Registration() {
			@Override
			public String getServiceId() {
				return MYSERVICE;
			}

			@Override
			public String getHost() {
				return "localhost";
			}

			@Override
			public int getPort() {
				return 80;
			}

			@Override
			public boolean isSecure() {
				return false;
			}

			@Override
			public URI getUri() {
				return null;
			}

			@Override
			public Map<String, String> getMetadata() {
				return null;
			}
		};

		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties, registration);

		LinkedHashMap<String, ZuulRoute> routes = routeLocator.locateRoutes();
		ZuulRoute actual = routes.get("/**");
		assertNull("routes didn't ignore " + MYSERVICE, actual);

		List<Route> routesMap = routeLocator.getRoutes();
		assertNotNull("routesMap was null", routesMap);
		assertTrue("routesMap was empty", routesMap.isEmpty());
	}

	@Test
	public void testIgnoredLocalServiceFalse() {
		this.properties.setIgnoreLocalService(false);

		given(this.discovery.getServices())
				.willReturn(Collections.singletonList(MYSERVICE));

		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties);

		List<Route> routesMap = routeLocator.getRoutes();
		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertMapping(routesMap, MYSERVICE);
	}

	@Test
	public void testLocalServiceExceptionIgnored() {
		given(this.discovery.getServices()).willReturn(Collections.<String>emptyList());

		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties, (Registration)null);

		// if no exception is thrown in constructor, this is a success
		routeLocator.locateRoutes();
	}

	@Test
	public void testRegExServiceRouteMapperNoServiceIdMatches() {
		given(this.discovery.getServices())
				.willReturn(Collections.singletonList(MYSERVICE));

		PatternServiceRouteMapper regExServiceRouteMapper = new PatternServiceRouteMapper(
				this.regexMapper.getServicePattern(), this.regexMapper.getRoutePattern());
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties, regExServiceRouteMapper);
		List<Route> routesMap = routeLocator.getRoutes();
		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertMapping(routesMap, MYSERVICE);
	}

	@Test
	public void testRegExServiceRouteMapperServiceIdMatches() {
		given(this.discovery.getServices())
				.willReturn(Collections.singletonList("rest-service-v1"));

		PatternServiceRouteMapper regExServiceRouteMapper = new PatternServiceRouteMapper(
				this.regexMapper.getServicePattern(), this.regexMapper.getRoutePattern());
		DiscoveryClientRouteLocator routeLocator = new DiscoveryClientRouteLocator("/",
				this.discovery, this.properties, regExServiceRouteMapper);
		List<Route> routesMap = routeLocator.getRoutes();
		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertMapping(routesMap, "rest-service-v1", "v1/rest-service");
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

	private void setTestRequestcontext() {
		RequestContext context = new RequestContext();
		RequestContext.testSetCurrentContext(context);
	}
}
