/*
 * Copyright 2017 the original author or authors.
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
 *
 */

package org.springframework.cloud.netflix.zuul.filters.post;

import com.netflix.util.Pair;
import com.netflix.zuul.context.RequestContext;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Biju Kunjummen
 */

public class LocationRewriteFilterTests {

	private final String ZUUL_HOST = "myzuul.com";
	private final String ZUUL_SCHEME = "https";
	private final int ZUUL_PORT = 8443;
	private final String ZUUL_BASE_URL = String.format("%s://%s:%d", ZUUL_SCHEME,
			ZUUL_HOST, ZUUL_PORT);

	private final String SERVER_HOST = "someserver.com";
	private final String SERVER_SCHEME = "http";
	private final int SERVER_PORT = 8564;
	private final String SERVER_BASE_URL = String.format("%s://%s:%d", SERVER_SCHEME,
			SERVER_HOST, SERVER_PORT);

	@Before
	public void before() {
		RequestContext context = new RequestContext();
		RequestContext.testSetCurrentContext(context);
	}

	@Test
	public void shouldRewriteLocationHeadersWithRoutePrefix() {
		RequestContext context = RequestContext.getCurrentContext();
		ZuulProperties zuulProperties = new ZuulProperties();
		LocationRewriteFilter filter = setFilterUpWith(context, zuulProperties,
				new Route("service1", "/redirectingUri", "service1", "prefix", false,
						Collections.EMPTY_SET, true),
				"/prefix/redirectingUri", "/redirectedUri;someparam?param1=abc");
		filter.run();
		assertThat(getLocationHeader(context).second()).isEqualTo(String
				.format("%s/prefix/redirectedUri;someparam?param1=abc", ZUUL_BASE_URL));
	}

	@Test
	public void shouldBeUntouchedIfNoRoutesFound() {
		RequestContext context = RequestContext.getCurrentContext();
		ZuulProperties zuulProperties = new ZuulProperties();
		LocationRewriteFilter filter = setFilterUpWith(context, zuulProperties, null,
				"/prefix/redirectingUri", "/redirectedUri;someparam?param1=abc");
		filter.run();
		assertThat(getLocationHeader(context).second()).isEqualTo(
				String.format("%s/redirectedUri;someparam?param1=abc", SERVER_BASE_URL));
	}

	@Test
	public void shouldRewriteLocationHeadersIfPrefixIsNotStripped() {
		RequestContext context = RequestContext.getCurrentContext();
		ZuulProperties zuulProperties = new ZuulProperties();
		LocationRewriteFilter filter = setFilterUpWith(context, zuulProperties,
				new Route("service1", "/something/redirectingUri", "service1", "prefix",
						false, Collections.EMPTY_SET, false),
				"/prefix/redirectingUri",
				"/something/redirectedUri;someparam?param1=abc");
		filter.run();
		assertThat(getLocationHeader(context).second()).isEqualTo(String.format(
				"%s/something/redirectedUri;someparam?param1=abc", ZUUL_BASE_URL));
	}

	@Test
	public void shouldRewriteLocationHeadersIfPrefixIsEmpty() {
		RequestContext context = RequestContext.getCurrentContext();
		ZuulProperties zuulProperties = new ZuulProperties();
		LocationRewriteFilter filter = setFilterUpWith(context, zuulProperties,
				new Route("service1", "/something/redirectingUri", "service1", "", false,
						Collections.EMPTY_SET, true),
				"/redirectingUri", "/something/redirectedUri;someparam?param1=abc");
		filter.run();
		assertThat(getLocationHeader(context).second()).isEqualTo(String.format(
				"%s/something/redirectedUri;someparam?param1=abc", ZUUL_BASE_URL));
	}

	@Test
	public void shouldAddBackGlobalPrefixIfPresent() {
		RequestContext context = RequestContext.getCurrentContext();
		ZuulProperties zuulProperties = new ZuulProperties();
		zuulProperties.setPrefix("global");
		zuulProperties.setStripPrefix(true);
		LocationRewriteFilter filter = setFilterUpWith(context, zuulProperties,
				new Route("service1", "/something/redirectingUri", "service1", "prefix",
						false, Collections.EMPTY_SET, true),
				"/global/prefix/redirectingUri",
				"/something/redirectedUri;someparam?param1=abc");
		filter.run();
		assertThat(getLocationHeader(context).second()).isEqualTo(String.format(
				"%s/global/prefix/something/redirectedUri;someparam?param1=abc",
				ZUUL_BASE_URL));
	}

	@Test
	public void shouldNotAddBackGlobalPrefixIfNotStripped() {
		RequestContext context = RequestContext.getCurrentContext();
		ZuulProperties zuulProperties = new ZuulProperties();
		zuulProperties.setPrefix("global");
		zuulProperties.setStripPrefix(false);
		LocationRewriteFilter filter = setFilterUpWith(context, zuulProperties,
				new Route("service1", "/something/redirectingUri", "service1", "prefix",
						false, Collections.EMPTY_SET, true),
				"/global/prefix/redirectingUri",
				"/global/something/redirectedUri;someparam?param1=abc");
		filter.run();
		assertThat(getLocationHeader(context).second()).isEqualTo(String.format(
				"%s/global/prefix/something/redirectedUri;someparam?param1=abc",
				ZUUL_BASE_URL));
	}

	private LocationRewriteFilter setFilterUpWith(RequestContext context,
			ZuulProperties zuulProperties, Route route, String toZuulRequestUri,
			String redirectedUri) {
		MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
		httpServletRequest.setRequestURI(toZuulRequestUri);
		httpServletRequest.setServerName(ZUUL_HOST);
		httpServletRequest.setScheme(ZUUL_SCHEME);
		httpServletRequest.setServerPort(ZUUL_PORT);
		context.setRequest(httpServletRequest);

		MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
		context.getZuulResponseHeaders().add(new Pair<>("Location",
				String.format("%s%s", SERVER_BASE_URL, redirectedUri)));
		context.setResponse(httpServletResponse);

		RouteLocator routeLocator = mock(RouteLocator.class);
		when(routeLocator.getMatchingRoute(toZuulRequestUri)).thenReturn(route);
		LocationRewriteFilter filter = new LocationRewriteFilter(zuulProperties,
				routeLocator);

		return filter;
	}

	private Pair<String, String> getLocationHeader(RequestContext ctx) {
		if (ctx.getZuulResponseHeaders() != null) {
			for (Pair<String, String> pair : ctx.getZuulResponseHeaders()) {
				if (pair.first().equals("Location")) {
					return pair;
				}
			}
		}
		return null;
	}
}
