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

package org.springframework.cloud.netflix.zuul.filters.pre;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.netflix.util.Pair;
import com.netflix.zuul.context.RequestContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;
import org.springframework.cloud.netflix.zuul.filters.discovery.DiscoveryClientRouteLocator;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.FORWARD_TO_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.REQUEST_URI_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

/**
 * @author Dave Syer
 */
public class PreDecorationFilterTests {

	private PreDecorationFilter filter;

	@Mock
	private DiscoveryClient discovery;

	private ZuulProperties properties = new ZuulProperties();

	private DiscoveryClientRouteLocator routeLocator;

	private MockHttpServletRequest request = new MockHttpServletRequest();

	private ProxyRequestHelper proxyRequestHelper = new ProxyRequestHelper();

	@Before
	public void init() {
		initMocks(this);
		this.properties = new ZuulProperties();
		this.routeLocator = new DiscoveryClientRouteLocator("/", this.discovery,
				this.properties);
		this.filter = new PreDecorationFilter(this.routeLocator, "/", this.properties,
				this.proxyRequestHelper);
		RequestContext ctx = RequestContext.getCurrentContext();
		ctx.clear();
		ctx.setRequest(this.request);
	}

	@After
	public void clear() {
		RequestContext.getCurrentContext().clear();
	}

	@Test
	public void basicProperties() throws Exception {
		assertEquals(5, this.filter.filterOrder());
		assertEquals(true, this.filter.shouldFilter());
		assertEquals(PRE_TYPE, this.filter.filterType());
	}

	@Test
	public void skippedIfServiceIdSet() throws Exception {
		RequestContext.getCurrentContext().set(SERVICE_ID_KEY, "myservice");
		assertEquals(false, this.filter.shouldFilter());
	}

	@Test
	public void skippedIfForwardToSet() throws Exception {
		RequestContext.getCurrentContext().set(FORWARD_TO_KEY, "myconteext");
		assertEquals(false, this.filter.shouldFilter());
	}

	@Test
	public void xForwardedHostHasPort() throws Exception {
		this.properties.setPrefix("/api");
		this.request.setRequestURI("/api/foo/1");
		this.request.setRemoteAddr("5.6.7.8");
		this.request.setServerPort(8080);
		this.routeLocator.addRoute(
				new ZuulRoute("foo", "/foo/**", "foo", null, false, null, null));
		this.filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();
		assertEquals("localhost:8080",
				ctx.getZuulRequestHeaders().get("x-forwarded-host"));
	}

	@Test
	public void xForwardedHostAndProtoAppend() throws Exception {
		this.properties.setPrefix("/api");
		this.request.setRequestURI("/api/foo/1");
		this.request.setRemoteAddr("5.6.7.8");
		this.request.setServerPort(8080);
		this.request.addHeader("X-Forwarded-Host", "example.com");
		this.request.addHeader("X-Forwarded-Proto", "https");
		this.routeLocator.addRoute(
				new ZuulRoute("foo", "/foo/**", "foo", null, false, null, null));
		this.filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();
		assertEquals("example.com,localhost:8080",
				ctx.getZuulRequestHeaders().get("x-forwarded-host"));
		assertEquals("443,8080", ctx.getZuulRequestHeaders().get("x-forwarded-port"));
		assertEquals("https,http", ctx.getZuulRequestHeaders().get("x-forwarded-proto"));
	}

	@Test
	public void xForwardedHostOnlyAppends() throws Exception {
		this.properties.setPrefix("/api");
		this.request.setRequestURI("/api/foo/1");
		this.request.setRemoteAddr("5.6.7.8");
		this.request.setServerPort(8080);
		this.request.addHeader("X-Forwarded-Host", "example.com");
		this.routeLocator.addRoute(
				new ZuulRoute("foo", "/foo/**", "foo", null, false, null, null));
		this.filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();
		assertEquals("example.com,localhost:8080",
				ctx.getZuulRequestHeaders().get("x-forwarded-host"));
		assertEquals("8080", ctx.getZuulRequestHeaders().get("x-forwarded-port"));
		assertEquals("http", ctx.getZuulRequestHeaders().get("x-forwarded-proto"));
	}

	@Test
	public void xForwardedProtoOnlyAppends() throws Exception {
		this.properties.setPrefix("/api");
		this.request.setRequestURI("/api/foo/1");
		this.request.setRemoteAddr("5.6.7.8");
		this.request.setServerPort(8080);
		this.request.addHeader("X-Forwarded-Proto", "https");
		this.routeLocator.addRoute(
				new ZuulRoute("foo", "/foo/**", "foo", null, false, null, null));
		this.filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();
		assertEquals("localhost:8080",
				ctx.getZuulRequestHeaders().get("x-forwarded-host"));
		assertEquals("443,8080", ctx.getZuulRequestHeaders().get("x-forwarded-port"));
		assertEquals("https,http", ctx.getZuulRequestHeaders().get("x-forwarded-proto"));
	}

	@Test
	public void xForwardedProtoHttpOnlyAppends() throws Exception {
		this.properties.setPrefix("/api");
		this.request.setRequestURI("/api/foo/1");
		this.request.setRemoteAddr("5.6.7.8");
		this.request.setServerPort(8080);
		this.request.addHeader("X-Forwarded-Proto", "http");
		this.routeLocator.addRoute(
				new ZuulRoute("foo", "/foo/**", "foo", null, false, null, null));
		this.filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();
		assertEquals("localhost:8080",
				ctx.getZuulRequestHeaders().get("x-forwarded-host"));
		assertEquals("80,8080", ctx.getZuulRequestHeaders().get("x-forwarded-port"));
		assertEquals("http,http", ctx.getZuulRequestHeaders().get("x-forwarded-proto"));
	}

	@Test
	public void xForwardedPortOnlyAppends() throws Exception {
		this.properties.setPrefix("/api");
		this.request.setRequestURI("/api/foo/1");
		this.request.setRemoteAddr("5.6.7.8");
		this.request.setServerPort(8080);
		this.request.addHeader("X-Forwarded-Port", "456");
		this.routeLocator.addRoute(
				new ZuulRoute("foo", "/foo/**", "foo", null, false, null, null));
		this.filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();
		assertEquals("localhost:8080",
				ctx.getZuulRequestHeaders().get("x-forwarded-host"));
		assertEquals("456,8080", ctx.getZuulRequestHeaders().get("x-forwarded-port"));
		assertEquals("http", ctx.getZuulRequestHeaders().get("x-forwarded-proto"));
	}

	@Test
	public void xForwardedPortAndProtoAppends() throws Exception {
		this.properties.setPrefix("/api");
		this.request.setRequestURI("/api/foo/1");
		this.request.setRemoteAddr("5.6.7.8");
		this.request.setServerPort(8080);
		this.request.addHeader("X-Forwarded-Proto", "https");
		this.request.addHeader("X-Forwarded-Port", "456");
		this.routeLocator.addRoute(
				new ZuulRoute("foo", "/foo/**", "foo", null, false, null, null));
		this.filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();
		assertEquals("localhost:8080",
				ctx.getZuulRequestHeaders().get("x-forwarded-host"));
		assertEquals("456,8080", ctx.getZuulRequestHeaders().get("x-forwarded-port"));
		assertEquals("https,http", ctx.getZuulRequestHeaders().get("x-forwarded-proto"));
	}

	@Test
	public void hostHeaderSet() throws Exception {
		this.properties.setPrefix("/api");
		this.properties.setAddHostHeader(true);
		this.request.setRequestURI("/api/foo/1");
		this.request.setRemoteAddr("5.6.7.8");
		this.request.setServerPort(8080);
		this.routeLocator.addRoute(
				new ZuulRoute("foo", "/foo/**", "foo", null, false, null, null));
		this.filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();
		assertEquals("localhost:8080",
				ctx.getZuulRequestHeaders().get("x-forwarded-host"));
		assertEquals("localhost:8080", ctx.getZuulRequestHeaders().get("host"));
	}

	@Test
	public void prefixRouteAddsHeader() throws Exception {
		this.properties.setPrefix("/api");
		this.properties.setStripPrefix(true);
		this.request.setRequestURI("/api/foo/1");
		this.request.setRemoteAddr("5.6.7.8");
		this.request.addHeader("X-Forwarded-For", "1.2.3.4");
		this.routeLocator.addRoute(
				new ZuulRoute("foo", "/foo/**", "foo", null, false, null, null));
		this.filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();
		assertEquals("/foo/1", ctx.get(REQUEST_URI_KEY));
		assertEquals("localhost", ctx.getZuulRequestHeaders().get("x-forwarded-host"));
		assertEquals("80", ctx.getZuulRequestHeaders().get("x-forwarded-port"));
		assertEquals("http", ctx.getZuulRequestHeaders().get("x-forwarded-proto"));
		assertEquals("/api", ctx.getZuulRequestHeaders().get("x-forwarded-prefix"));
		assertEquals("1.2.3.4, 5.6.7.8",
				ctx.getZuulRequestHeaders().get("x-forwarded-for"));
		assertEquals("foo",
				getHeader(ctx.getOriginResponseHeaders(), "x-zuul-serviceid"));
	}

	@Test
	public void prefixRouteWithPrefixHeaderConcatsHeader() throws Exception {
		this.properties.setPrefix("/api");
		this.properties.setStripPrefix(true);
		this.request.setRequestURI("/api/foo/1");
		this.request.setRemoteAddr("5.6.7.8");
		this.request.addHeader("X-Forwarded-For", "1.2.3.4");
		this.request.addHeader("X-Forwarded-Prefix", "/prefix");
		this.routeLocator.addRoute(
				new ZuulRoute("foo", "/foo/**", "foo", null, false, null, null));
		this.filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();
		assertEquals("/foo/1", ctx.get(REQUEST_URI_KEY));
		assertEquals("localhost", ctx.getZuulRequestHeaders().get("x-forwarded-host"));
		assertEquals("80", ctx.getZuulRequestHeaders().get("x-forwarded-port"));
		assertEquals("http", ctx.getZuulRequestHeaders().get("x-forwarded-proto"));
		assertEquals("/prefix/api",
				ctx.getZuulRequestHeaders().get("x-forwarded-prefix"));
		assertEquals("1.2.3.4, 5.6.7.8",
				ctx.getZuulRequestHeaders().get("x-forwarded-for"));
		assertEquals("foo",
				getHeader(ctx.getOriginResponseHeaders(), "x-zuul-serviceid"));
	}

	@Test
	public void routeWithContextPath() {
		this.properties.setStripPrefix(false);
		this.request.setRequestURI("/api/foo/1");
		this.request.setContextPath("/context-path");
		this.routeLocator.addRoute(
				new ZuulRoute("foo", "/api/foo/**", "foo", null, false, null, null));
		this.filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();
		assertEquals("/api/foo/1", ctx.get(REQUEST_URI_KEY));
		assertEquals("localhost", ctx.getZuulRequestHeaders().get("x-forwarded-host"));
		assertEquals("80", ctx.getZuulRequestHeaders().get("x-forwarded-port"));
		assertEquals("http", ctx.getZuulRequestHeaders().get("x-forwarded-proto"));
		assertEquals("/context-path",
				ctx.getZuulRequestHeaders().get("x-forwarded-prefix"));
		assertEquals("foo",
				getHeader(ctx.getOriginResponseHeaders(), "x-zuul-serviceid"));
	}

	@Test
	public void prefixRouteWithContextPath() {
		this.properties.setPrefix("/api");
		this.properties.setStripPrefix(true);
		this.request.setRequestURI("/api/foo/1");
		this.request.setContextPath("/context-path");
		this.routeLocator.addRoute(
				new ZuulRoute("foo", "/foo/**", "foo", null, false, null, null));
		this.filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();
		assertEquals("/foo/1", ctx.get(REQUEST_URI_KEY));
		assertEquals("localhost", ctx.getZuulRequestHeaders().get("x-forwarded-host"));
		assertEquals("80", ctx.getZuulRequestHeaders().get("x-forwarded-port"));
		assertEquals("http", ctx.getZuulRequestHeaders().get("x-forwarded-proto"));
		assertEquals("/context-path/api",
				ctx.getZuulRequestHeaders().get("x-forwarded-prefix"));
		assertEquals("foo",
				getHeader(ctx.getOriginResponseHeaders(), "x-zuul-serviceid"));
	}

	@Test
	public void routeIgnoreContextPathIfPrefixHeader() {
		this.properties.setStripPrefix(false);
		this.request.setRequestURI("/api/foo/1");
		this.request.setContextPath("/context-path");
		this.request.addHeader("X-Forwarded-Prefix", "/prefix");
		this.routeLocator.addRoute(
				new ZuulRoute("foo", "/api/foo/**", "foo", null, false, null, null));
		this.filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();
		assertEquals("/api/foo/1", ctx.get(REQUEST_URI_KEY));
		assertEquals("localhost", ctx.getZuulRequestHeaders().get("x-forwarded-host"));
		assertEquals("80", ctx.getZuulRequestHeaders().get("x-forwarded-port"));
		assertEquals("http", ctx.getZuulRequestHeaders().get("x-forwarded-proto"));
		assertEquals("/prefix", ctx.getZuulRequestHeaders().get("x-forwarded-prefix"));
		assertEquals("foo",
				getHeader(ctx.getOriginResponseHeaders(), "x-zuul-serviceid"));
	}

	@Test
	public void prefixRouteIgnoreContextPathIfPrefixHeader() {
		this.properties.setPrefix("/api");
		this.properties.setStripPrefix(true);
		this.request.setRequestURI("/api/foo/1");
		this.request.setContextPath("/context-path");
		this.request.addHeader("X-Forwarded-Prefix", "/prefix");
		this.routeLocator.addRoute(
				new ZuulRoute("foo", "/foo/**", "foo", null, false, null, null));
		this.filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();
		assertEquals("/foo/1", ctx.get(REQUEST_URI_KEY));
		assertEquals("localhost", ctx.getZuulRequestHeaders().get("x-forwarded-host"));
		assertEquals("80", ctx.getZuulRequestHeaders().get("x-forwarded-port"));
		assertEquals("http", ctx.getZuulRequestHeaders().get("x-forwarded-proto"));
		assertEquals("/prefix/api",
				ctx.getZuulRequestHeaders().get("x-forwarded-prefix"));
		assertEquals("foo",
				getHeader(ctx.getOriginResponseHeaders(), "x-zuul-serviceid"));
	}

	@Test
	public void forwardRouteAddsLocation() throws Exception {
		this.properties.setPrefix("/api");
		this.properties.setStripPrefix(true);
		this.request.setRequestURI("/api/foo/1");
		this.routeLocator.addRoute(
				new ZuulRoute("foo", "/foo/**", null, "forward:/foo", true, null, null));
		this.filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();
		assertEquals("/foo/1", ctx.get(FORWARD_TO_KEY));
	}

	@Test
	public void forwardWithoutStripPrefixAppendsPath() throws Exception {
		this.request.setRequestURI("/foo/1");
		this.routeLocator.addRoute(
				new ZuulRoute("foo", "/foo/**", null, "forward:/bar", false, null, null));
		this.filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();
		assertEquals("/bar/foo/1", ctx.get(FORWARD_TO_KEY));
	}

	@Test
	public void prefixRouteWithRouteStrippingAddsHeader() throws Exception {
		this.properties.setPrefix("/api");
		this.properties.setStripPrefix(true);
		this.request.setRequestURI("/api/foo/1");
		this.routeLocator.addRoute("/foo/**", "foo");
		this.filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();
		assertEquals("/1", ctx.get(REQUEST_URI_KEY));
		assertEquals("localhost", ctx.getZuulRequestHeaders().get("x-forwarded-host"));
		assertEquals("http", ctx.getZuulRequestHeaders().get("x-forwarded-proto"));
		assertEquals("/api/foo", ctx.getZuulRequestHeaders().get("x-forwarded-prefix"));
		assertEquals("foo",
				getHeader(ctx.getOriginResponseHeaders(), "x-zuul-serviceid"));
	}

	@Test
	public void routeNotFound() throws Exception {
		this.properties.setPrefix("/api");
		this.properties.setStripPrefix(true);
		this.routeLocator.addRoute(
				new ZuulRoute("foo", "/foo/**", null, "forward:/foo", true, null, null));

		this.request.setRequestURI("/api/bar/1");

		this.filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();
		assertEquals("/api/bar/1", ctx.get(FORWARD_TO_KEY));
	}

	@Test
	public void routeNotFoundDispatcherServletSpecialPath() throws Exception {
		this.properties.setPrefix("/api");
		this.properties.setStripPrefix(true);
		this.properties.setAddProxyHeaders(true);
		this.routeLocator.addRoute(
				new ZuulRoute("foo", "/foo/**", null, "forward:/foo", true, null, null));

		this.filter = new PreDecorationFilter(this.routeLocator, "/special",
				this.properties, this.proxyRequestHelper);

		this.request.setRequestURI("/api/bar/1");

		this.filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();
		assertEquals("/special/api/bar/1", ctx.get(FORWARD_TO_KEY));
	}

	@Test
	public void routeNotFoundZuulRequest() throws Exception {
		setTestRequestContext();
		RequestContext ctx = RequestContext.getCurrentContext();
		RequestContext.getCurrentContext().setZuulEngineRan();
		this.request.setRequestURI("/zuul/api/bar/1");
		ctx.setRequest(this.request);

		this.properties.setPrefix("/api");
		this.properties.setStripPrefix(true);
		this.properties.setServletPath("/zuul");
		this.routeLocator.addRoute(
				new ZuulRoute("foo", "/foo/**", null, "forward:/foo", true, null, null));

		this.filter.run();

		assertEquals("/api/bar/1", ctx.get(FORWARD_TO_KEY));
	}

	@Test
	public void routeNotFoundZuulRequestDispatcherServletSpecialPath() throws Exception {
		setTestRequestContext();
		RequestContext ctx = RequestContext.getCurrentContext();
		RequestContext.getCurrentContext().setZuulEngineRan();
		this.request.setRequestURI("/zuul/api/bar/1");
		ctx.setRequest(this.request);

		this.properties.setPrefix("/api");
		this.properties.setStripPrefix(true);
		this.properties.setServletPath("/zuul");
		this.properties.setAddProxyHeaders(true);
		this.routeLocator.addRoute(
				new ZuulRoute("foo", "/foo/**", null, "forward:/foo", true, null, null));
		this.filter = new PreDecorationFilter(this.routeLocator, "/special",
				this.properties, this.proxyRequestHelper);

		this.filter.run();

		assertEquals("/special/api/bar/1", ctx.get(FORWARD_TO_KEY));
	}

	@Test
	public void routeNotFoundZuulRequestZuulHomeMapping() throws Exception {
		setTestRequestContext();
		RequestContext ctx = RequestContext.getCurrentContext();
		RequestContext.getCurrentContext().setZuulEngineRan();
		this.request.setRequestURI("/api/bar/1");
		ctx.setRequest(this.request);

		this.properties.setPrefix("/api");
		this.properties.setStripPrefix(true);
		this.properties.setServletPath("/");
		this.properties.setAddProxyHeaders(true);
		this.routeLocator.addRoute(
				new ZuulRoute("foo", "/foo/**", null, "forward:/foo", true, null, null));

		this.filter = new PreDecorationFilter(this.routeLocator, "/special",
				this.properties, this.proxyRequestHelper);

		this.filter.run();

		assertEquals("/special/api/bar/1", ctx.get(FORWARD_TO_KEY));
	}

	@Test
	public void sensitiveHeadersOverride() throws Exception {
		this.properties.setPrefix("/api");
		this.properties.setStripPrefix(true);
		this.properties.setSensitiveHeaders(Collections.singleton("x-bar"));
		this.request.setRequestURI("/api/foo/1");
		ZuulRoute route = new ZuulRoute("/foo/**", "foo");
		route.setSensitiveHeaders(Collections.singleton("x-foo"));
		this.routeLocator.addRoute(route);
		this.filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();
		@SuppressWarnings("unchecked")
		Set<String> sensitiveHeaders = (Set<String>) ctx
				.get(ProxyRequestHelper.IGNORED_HEADERS);
		assertTrue("sensitiveHeaders is wrong: " + sensitiveHeaders,
				sensitiveHeaders.containsAll(Collections.singletonList("x-foo")));
		assertFalse("sensitiveHeaders is wrong: " + sensitiveHeaders,
				sensitiveHeaders.contains("Cookie"));
	}

	@Test
	public void sensitiveHeadersOverrideEmpty() throws Exception {
		this.properties.setPrefix("/api");
		this.properties.setStripPrefix(true);
		this.properties.setSensitiveHeaders(Collections.singleton("x-bar"));
		this.request.setRequestURI("/api/foo/1");
		ZuulRoute route = new ZuulRoute("/foo/**", "foo");
		route.setSensitiveHeaders(Collections.<String>emptySet());
		this.routeLocator.addRoute(route);
		this.filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();
		@SuppressWarnings("unchecked")
		Set<String> sensitiveHeaders = (Set<String>) ctx
				.get(ProxyRequestHelper.IGNORED_HEADERS);
		assertTrue("sensitiveHeaders is wrong: " + sensitiveHeaders,
				sensitiveHeaders.isEmpty());
	}

	@Test
	public void sensitiveHeadersDefaults() throws Exception {
		this.properties.setPrefix("/api");
		this.properties.setStripPrefix(true);
		this.properties.setSensitiveHeaders(Collections.singleton("x-bar"));
		this.request.setRequestURI("/api/foo/1");
		this.routeLocator.addRoute("/foo/**", "foo");
		this.filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();
		@SuppressWarnings("unchecked")
		Set<String> sensitiveHeaders = (Set<String>) ctx
				.get(ProxyRequestHelper.IGNORED_HEADERS);
		assertTrue("sensitiveHeaders is wrong: " + sensitiveHeaders,
				sensitiveHeaders.containsAll(Collections.singletonList("x-bar")));
		assertFalse("sensitiveHeaders is wrong", sensitiveHeaders.contains("Cookie"));
	}

	@Test
	public void sensitiveHeadersCaseInsensitive() throws Exception {
		this.properties.setPrefix("/api");
		this.properties.setStripPrefix(true);
		this.properties.setSensitiveHeaders(Collections.singleton("X-bAr"));
		this.request.setRequestURI("/api/foo/1");
		this.routeLocator.addRoute("/foo/**", "foo");
		this.filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();
		@SuppressWarnings("unchecked")
		Set<String> sensitiveHeaders = (Set<String>) ctx
				.get(ProxyRequestHelper.IGNORED_HEADERS);
		assertTrue("sensitiveHeaders is wrong: " + sensitiveHeaders,
				sensitiveHeaders.containsAll(Collections.singletonList("x-bar")));
	}

	@Test
	public void sensitiveHeadersOverrideCaseInsensitive() throws Exception {
		this.properties.setPrefix("/api");
		this.properties.setStripPrefix(true);
		this.properties.setSensitiveHeaders(Collections.singleton("X-bAr"));
		this.request.setRequestURI("/api/foo/1");
		ZuulRoute route = new ZuulRoute("/foo/**", "foo");
		route.setSensitiveHeaders(Collections.singleton("X-Foo"));
		this.routeLocator.addRoute(route);
		this.filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();
		@SuppressWarnings("unchecked")
		Set<String> sensitiveHeaders = (Set<String>) ctx
				.get(ProxyRequestHelper.IGNORED_HEADERS);
		assertTrue("sensitiveHeaders is wrong: " + sensitiveHeaders,
				sensitiveHeaders.containsAll(Collections.singletonList("x-foo")));
	}

	@Test
	public void ignoredHeadersAlreadySetInRequestContextDontGetOverridden()
			throws Exception {
		this.properties.setPrefix("/api");
		this.properties.setStripPrefix(true);
		this.properties.setSensitiveHeaders(Collections.singleton("x-bar"));
		this.request.setRequestURI("/api/foo/1");
		this.routeLocator.addRoute("/foo/**", "foo");
		RequestContext ctx = RequestContext.getCurrentContext();
		ctx.set(ProxyRequestHelper.IGNORED_HEADERS,
				new HashSet<>(Arrays.asList("x-foo")));
		this.filter.run();
		@SuppressWarnings("unchecked")
		Set<String> sensitiveHeaders = (Set<String>) ctx
				.get(ProxyRequestHelper.IGNORED_HEADERS);
		assertTrue("sensitiveHeaders is wrong: " + sensitiveHeaders,
				sensitiveHeaders.containsAll(Arrays.asList("x-bar", "x-foo")));
	}

	@Test
	public void urlProperlyDecodedWhenCharacterEncodingIsSet() throws Exception {
		this.request.setCharacterEncoding("UTF-8");
		this.properties.setPrefix("/api");
		this.properties.setStripPrefix(true);
		this.request.setRequestURI("/api/foo/ol%C3%A9%D7%93%D7%A8%D7%A2%D7%A7");
		this.routeLocator.addRoute("/foo/**", "foo");
		RequestContext ctx = RequestContext.getCurrentContext();
		this.filter.run();
		String decodedRequestURI = (String) ctx.get(REQUEST_URI_KEY);
		assertTrue(decodedRequestURI.equals("/oléדרעק"));
	}

	private Object getHeader(List<Pair<String, String>> headers, String key) {
		String value = null;
		for (Pair<String, String> pair : headers) {
			if (pair.first().toLowerCase().equals(key.toLowerCase())) {
				value = pair.second();
				break;
			}
		}
		return value;
	}

	private void setTestRequestContext() {
		RequestContext context = new RequestContext();
		RequestContext.testSetCurrentContext(context);

	}

}
