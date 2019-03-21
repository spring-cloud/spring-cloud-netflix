/*
 * Copyright 2013-2019 the original author or authors.
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
import org.junit.runner.RunWith;
import org.mockito.Mock;

import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;
import org.springframework.cloud.netflix.zuul.filters.discovery.DiscoveryClientRouteLocator;
import org.springframework.cloud.test.ClassPathExclusions;
import org.springframework.cloud.test.ModifiedClassPathRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.FORWARD_TO_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.REQUEST_URI_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

/**
 * @author Dave Syer
 */
@RunWith(ModifiedClassPathRunner.class)
// This is needed for sensitiveHeadersOverrideEmpty, if Spring Security is on the
// classpath
// then sensitive headers will always be present.
@ClassPathExclusions({ "spring-security-*.jar" })
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
		this.proxyRequestHelper = new ProxyRequestHelper(properties);
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
		assertThat(this.filter.filterOrder()).isEqualTo(5);
		assertThat(this.filter.shouldFilter()).isEqualTo(true);
		assertThat(this.filter.filterType()).isEqualTo(PRE_TYPE);
	}

	@Test
	public void skippedIfServiceIdSet() throws Exception {
		RequestContext.getCurrentContext().set(SERVICE_ID_KEY, "myservice");
		assertThat(this.filter.shouldFilter()).isEqualTo(false);
	}

	@Test
	public void skippedIfForwardToSet() throws Exception {
		RequestContext.getCurrentContext().set(FORWARD_TO_KEY, "myconteext");
		assertThat(this.filter.shouldFilter()).isEqualTo(false);
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
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-host"))
				.isEqualTo("localhost:8080");
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
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-host"))
				.isEqualTo("example.com,localhost:8080");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-port"))
				.isEqualTo("443,8080");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-proto"))
				.isEqualTo("https,http");
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
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-host"))
				.isEqualTo("example.com,localhost:8080");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-port")).isEqualTo("8080");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-proto"))
				.isEqualTo("http");
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
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-host"))
				.isEqualTo("localhost:8080");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-port"))
				.isEqualTo("443,8080");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-proto"))
				.isEqualTo("https,http");
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
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-host"))
				.isEqualTo("localhost:8080");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-port"))
				.isEqualTo("80,8080");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-proto"))
				.isEqualTo("http,http");
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
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-host"))
				.isEqualTo("localhost:8080");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-port"))
				.isEqualTo("456,8080");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-proto"))
				.isEqualTo("http");
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
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-host"))
				.isEqualTo("localhost:8080");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-port"))
				.isEqualTo("456,8080");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-proto"))
				.isEqualTo("https,http");
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
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-host"))
				.isEqualTo("localhost:8080");
		assertThat(ctx.getZuulRequestHeaders().get("host")).isEqualTo("localhost:8080");
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
		assertThat(ctx.get(REQUEST_URI_KEY)).isEqualTo("/foo/1");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-host"))
				.isEqualTo("localhost");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-port")).isEqualTo("80");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-proto"))
				.isEqualTo("http");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-prefix"))
				.isEqualTo("/api");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-for"))
				.isEqualTo("1.2.3.4, 5.6.7.8");
		assertThat(getHeader(ctx.getOriginResponseHeaders(), "x-zuul-serviceid"))
				.isEqualTo("foo");
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
		assertThat(ctx.get(REQUEST_URI_KEY)).isEqualTo("/foo/1");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-host"))
				.isEqualTo("localhost");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-port")).isEqualTo("80");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-proto"))
				.isEqualTo("http");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-prefix"))
				.isEqualTo("/prefix/api");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-for"))
				.isEqualTo("1.2.3.4, 5.6.7.8");
		assertThat(getHeader(ctx.getOriginResponseHeaders(), "x-zuul-serviceid"))
				.isEqualTo("foo");
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
		assertThat(ctx.get(REQUEST_URI_KEY)).isEqualTo("/api/foo/1");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-host"))
				.isEqualTo("localhost");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-port")).isEqualTo("80");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-proto"))
				.isEqualTo("http");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-prefix"))
				.isEqualTo("/context-path");
		assertThat(getHeader(ctx.getOriginResponseHeaders(), "x-zuul-serviceid"))
				.isEqualTo("foo");
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
		assertThat(ctx.get(REQUEST_URI_KEY)).isEqualTo("/foo/1");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-host"))
				.isEqualTo("localhost");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-port")).isEqualTo("80");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-proto"))
				.isEqualTo("http");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-prefix"))
				.isEqualTo("/context-path/api");
		assertThat(getHeader(ctx.getOriginResponseHeaders(), "x-zuul-serviceid"))
				.isEqualTo("foo");
	}

	@Test
	public void dontDecodeUrl() {
		this.properties.setPrefix("/api");
		this.properties.setStripPrefix(true);
		this.properties.setDecodeUrl(false);
		this.request.setRequestURI("/api/foo/encoded%2Fpath");
		this.request.setContextPath("/context-path");
		this.routeLocator.addRoute(
				new ZuulRoute("foo", "/foo/**", "foo", null, false, null, null));
		this.filter = new PreDecorationFilter(this.routeLocator, "/", this.properties,
				this.proxyRequestHelper);
		this.filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();
		assertThat(ctx.get(REQUEST_URI_KEY)).isEqualTo("/foo/encoded%2Fpath");
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
		assertThat(ctx.get(REQUEST_URI_KEY)).isEqualTo("/api/foo/1");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-host"))
				.isEqualTo("localhost");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-port")).isEqualTo("80");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-proto"))
				.isEqualTo("http");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-prefix"))
				.isEqualTo("/prefix");
		assertThat(getHeader(ctx.getOriginResponseHeaders(), "x-zuul-serviceid"))
				.isEqualTo("foo");
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
		assertThat(ctx.get(REQUEST_URI_KEY)).isEqualTo("/foo/1");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-host"))
				.isEqualTo("localhost");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-port")).isEqualTo("80");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-proto"))
				.isEqualTo("http");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-prefix"))
				.isEqualTo("/prefix/api");
		assertThat(getHeader(ctx.getOriginResponseHeaders(), "x-zuul-serviceid"))
				.isEqualTo("foo");
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
		assertThat(ctx.get(FORWARD_TO_KEY)).isEqualTo("/foo/1");
	}

	@Test
	public void forwardWithoutStripPrefixAppendsPath() throws Exception {
		this.request.setRequestURI("/foo/1");
		this.routeLocator.addRoute(
				new ZuulRoute("foo", "/foo/**", null, "forward:/bar", false, null, null));
		this.filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();
		assertThat(ctx.get(FORWARD_TO_KEY)).isEqualTo("/bar/foo/1");
	}

	@Test
	public void prefixRouteWithRouteStrippingAddsHeader() throws Exception {
		this.properties.setPrefix("/api");
		this.properties.setStripPrefix(true);
		this.request.setRequestURI("/api/foo/1");
		this.routeLocator.addRoute("/foo/**", "foo");
		this.filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();
		assertThat(ctx.get(REQUEST_URI_KEY)).isEqualTo("/1");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-host"))
				.isEqualTo("localhost");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-proto"))
				.isEqualTo("http");
		assertThat(ctx.getZuulRequestHeaders().get("x-forwarded-prefix"))
				.isEqualTo("/api/foo");
		assertThat(getHeader(ctx.getOriginResponseHeaders(), "x-zuul-serviceid"))
				.isEqualTo("foo");
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
		assertThat(ctx.get(FORWARD_TO_KEY)).isEqualTo("/api/bar/1");
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
		assertThat(ctx.get(FORWARD_TO_KEY)).isEqualTo("/special/api/bar/1");
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

		assertThat(ctx.get(FORWARD_TO_KEY)).isEqualTo("/api/bar/1");
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

		assertThat(ctx.get(FORWARD_TO_KEY)).isEqualTo("/special/api/bar/1");
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

		assertThat(ctx.get(FORWARD_TO_KEY)).isEqualTo("/special/api/bar/1");
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
		assertThat(sensitiveHeaders.containsAll(Collections.singletonList("x-foo")))
				.as("sensitiveHeaders is wrong: " + sensitiveHeaders).isTrue();
		assertThat(sensitiveHeaders.contains("Cookie"))
				.as("sensitiveHeaders is wrong: " + sensitiveHeaders).isFalse();
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
		assertThat(sensitiveHeaders.isEmpty())
				.as("sensitiveHeaders is wrong: " + sensitiveHeaders).isTrue();
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
		assertThat(sensitiveHeaders.containsAll(Collections.singletonList("x-bar")))
				.as("sensitiveHeaders is wrong: " + sensitiveHeaders).isTrue();
		assertThat(sensitiveHeaders.contains("Cookie")).as("sensitiveHeaders is wrong")
				.isFalse();
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
		assertThat(sensitiveHeaders.containsAll(Collections.singletonList("x-bar")))
				.as("sensitiveHeaders is wrong: " + sensitiveHeaders).isTrue();
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
		assertThat(sensitiveHeaders.containsAll(Collections.singletonList("x-foo")))
				.as("sensitiveHeaders is wrong: " + sensitiveHeaders).isTrue();
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
		assertThat(sensitiveHeaders.containsAll(Arrays.asList("x-bar", "x-foo")))
				.as("sensitiveHeaders is wrong: " + sensitiveHeaders).isTrue();
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
		assertThat(decodedRequestURI.equals("/oléדרעק")).isTrue();
	}

	@Test
	public void headersAreProperlyIgnored() throws Exception {
		proxyRequestHelper.addIgnoredHeaders("x-forwarded-host", "x-forwarded-port");
		request.addHeader("x-forwarded-host", "B,127.0.0.1:8080");
		request.addHeader("x-forwarded-port", "A,8080");
		request.addHeader("x-forwarded-proto", "C,http");

		MultiValueMap<String, String> result = proxyRequestHelper
				.buildZuulRequestHeaders(request);

		assertThat(result.containsKey("x-forwarded-proto")).isTrue();
		assertThat(result.containsKey("x-forwarded-host")).isFalse();
		assertThat(result.containsKey("x-forwarded-port")).isFalse();
	}

	@Test
	public void nullDispatcherServletPath() {
		this.filter = new PreDecorationFilter(this.routeLocator, null, this.properties,
				this.proxyRequestHelper);

		String forwardUri = this.filter.getForwardUri("/mypath");
		assertThat(forwardUri).isEqualTo("/mypath");
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
