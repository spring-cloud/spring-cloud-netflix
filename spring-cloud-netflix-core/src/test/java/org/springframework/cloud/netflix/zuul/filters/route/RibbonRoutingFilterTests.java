/*
 * Copyright 2013-2017 the original author or authors.
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

package org.springframework.cloud.netflix.zuul.filters.route;

import java.util.Collections;

import com.netflix.zuul.context.RequestContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.netflix.ribbon.support.RibbonRequestCustomizer;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.LOAD_BALANCER_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

/**
 * @author Spencer Gibb
 * @author Yongsung Yoon
 */
public class RibbonRoutingFilterTests {

	private RequestContext requestContext;
	private RibbonRoutingFilter filter;

	@Before
	public void setUp() throws Exception {
		setUpRequestContext();
		setupRibbonRoutingFilter();
	}

	@After
	public void tearDown() throws Exception {
		requestContext.unset();
	}

	@Test
	public void useServlet31Works() {
		assertThat(filter.isUseServlet31()).isTrue();
	}

	@Test
	public void testLoadBalancerKeyToRibbonCommandContext() throws Exception {
		final String testKey = "testLoadBalancerKey";
		requestContext.set(LOAD_BALANCER_KEY, testKey);
		RibbonCommandContext commandContext = filter.buildCommandContext(requestContext);

		assertThat(commandContext.getLoadBalancerKey()).isEqualTo(testKey);
	}

	@Test
	public void testNullLoadBalancerKeyToRibbonCommandContext() throws Exception {
		requestContext.set(LOAD_BALANCER_KEY, null);
		RibbonCommandContext commandContext = filter.buildCommandContext(requestContext);

		assertThat(commandContext.getLoadBalancerKey()).isNull();
	}

	private void setUpRequestContext() {
		requestContext = RequestContext.getCurrentContext();
		MockHttpServletRequest mockRequest = new MockHttpServletRequest();
		mockRequest.setMethod("GET");
		mockRequest.setRequestURI("/foo/bar");
		requestContext.setRequest(mockRequest);
		requestContext.setRequestQueryParams(Collections.EMPTY_MAP);
		requestContext.set(SERVICE_ID_KEY, "testServiceId");
	}

	private void setupRibbonRoutingFilter() {
		RibbonCommandFactory factory = mock(RibbonCommandFactory.class);
		filter = new RibbonRoutingFilter(new ProxyRequestHelper(), factory, Collections.<RibbonRequestCustomizer>emptyList());
	}
}
