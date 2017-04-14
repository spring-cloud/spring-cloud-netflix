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
import com.netflix.zuul.monitoring.MonitoringHelper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
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

	@Before
	public void before() {
		MonitoringHelper.initMocks();
		RequestContext context = new RequestContext();
		RequestContext.testSetCurrentContext(context);
	}

	@Test
	public void shouldRewriteLocationHeadersWithPrefixIfForwardedWithNoPrefix() {
		RequestContext context = RequestContext.getCurrentContext();

		MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
		httpServletRequest.setRequestURI("/service1/redirectingUri");
		httpServletRequest.setServerName("myzuul.com");
		httpServletRequest.setScheme("https");
		httpServletRequest.setServerPort(8443);
		context.setRequest(httpServletRequest);

		MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
		context.getZuulResponseHeaders().add(new Pair<>("Location",
				"http://some.server.com:8564/redirectedUri;someparam?param1=abc"));
		context.setResponse(httpServletResponse);
		LocationRewriteFilter filter = createFilterWithRoute(context,
				new Route("service1", "/redirectingUri", "service1", "prefix", false,
						Collections.EMPTY_SET, true));
		filter.run();
		assertThat(getLocationHeader(context).second()).isEqualTo(
				"https://myzuul.com:8443/prefix/redirectedUri;someparam?param1=abc");
	}

	@Test
	public void shouldRewriteLocationHeadersNoAdditionalPrefixIfPrefixIsNotStripped() {
		RequestContext context = RequestContext.getCurrentContext();

		MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
		httpServletRequest.setRequestURI("/service1/redirectingUri");
		httpServletRequest.setServerName("myzuul.com");
		httpServletRequest.setScheme("https");
		httpServletRequest.setServerPort(8443);
		context.setRequest(httpServletRequest);

		MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
		context.getZuulResponseHeaders().add(new Pair<>("Location",
				"http://some.server.com:8564/something/redirectedUri;someparam?param1=abc"));
		context.setResponse(httpServletResponse);
		LocationRewriteFilter filter = createFilterWithRoute(context,
				new Route("service1", "/something/redirectingUri", "service1", "prefix",
						false, Collections.EMPTY_SET, false));
		filter.run();
		assertThat(getLocationHeader(context).second()).isEqualTo(
				"https://myzuul.com:8443/something/redirectedUri;someparam?param1=abc");
	}

	private LocationRewriteFilter createFilterWithRoute(RequestContext context,
			Route route) {
		RequestContext.testSetCurrentContext(context);
		RouteLocator routeLocator = mock(RouteLocator.class);
		when(routeLocator.getMatchingRoute("/service1/redirectingUri")).thenReturn(route);
		LocationRewriteFilter filter = new LocationRewriteFilter(routeLocator);

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
