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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import com.netflix.zuul.context.RequestContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.netflix.ribbon.support.RibbonRequestCustomizer;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.LOAD_BALANCER_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

/**
 * @author Spencer Gibb
 * @author Yongsung Yoon
 * @author Gang Li
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

	@Test
	public void testSetResponseWithNonHttpStatusCode() throws Exception {
		ClientHttpResponse response = this.createClientHttpResponseWithNonStatus();
		this.filter.setResponse(response);
		assertThat(517).isEqualTo(this.requestContext.get("responseStatusCode"));
	}

	@Test
	public void testSetResponseWithHttpStatusCode() throws Exception {
		ClientHttpResponse response = this.createClientHttpResponse();
		this.filter.setResponse(response);
		assertThat(200).isEqualTo(this.requestContext.get("responseStatusCode"));
	}

	private void setUpRequestContext() {
		requestContext = RequestContext.getCurrentContext();
		MockHttpServletRequest mockRequest = new MockHttpServletRequest();
		HttpServletResponse httpServletResponse = new MockHttpServletResponse();
		mockRequest.setMethod("GET");
		mockRequest.setRequestURI("/foo/bar");
		requestContext.setRequest(mockRequest);
		requestContext.setRequestQueryParams(Collections.EMPTY_MAP);
		requestContext.set(SERVICE_ID_KEY, "testServiceId");
		requestContext.set("response", httpServletResponse);
	}

	private void setupRibbonRoutingFilter() {
		RibbonCommandFactory factory = mock(RibbonCommandFactory.class);
		filter = new RibbonRoutingFilter(new ProxyRequestHelper(), factory, Collections.<RibbonRequestCustomizer>emptyList());
	}

	private ClientHttpResponse createClientHttpResponseWithNonStatus() {
		return new ClientHttpResponse() {
			@Override
			public HttpStatus getStatusCode() throws IOException {
				return null;
			}

			@Override
			public int getRawStatusCode() throws IOException {
				return 517;
			}

			@Override
			public String getStatusText() throws IOException {
				return "Fail";
			}

			@Override
			public void close() {

			}

			@Override
			public InputStream getBody() throws IOException {
				return new ByteArrayInputStream("Fail".getBytes());
			}

			@Override
			public HttpHeaders getHeaders() {
				HttpHeaders httpHeaders = new HttpHeaders();
				httpHeaders.setContentType(MediaType.APPLICATION_JSON);
				return httpHeaders;
			}
		};
	}

	private ClientHttpResponse createClientHttpResponse() {
		return new ClientHttpResponse() {
			@Override
			public HttpStatus getStatusCode() throws IOException {
				return HttpStatus.OK;
			}

			@Override
			public int getRawStatusCode() throws IOException {
				return 200;
			}

			@Override
			public String getStatusText() throws IOException {
				return "OK";
			}

			@Override
			public void close() {

			}

			@Override
			public InputStream getBody() throws IOException {
				return new ByteArrayInputStream("OK".getBytes());
			}

			@Override
			public HttpHeaders getHeaders() {
				HttpHeaders httpHeaders = new HttpHeaders();
				httpHeaders.setContentType(MediaType.APPLICATION_JSON);
				return httpHeaders;
			}
		};
	}
}
