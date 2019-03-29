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

package org.springframework.cloud.netflix.ribbon.support;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Ryan Baxter
 */
public class ContextAwareRequestTests {

	private RibbonCommandContext context;

	private ContextAwareRequest request;

	@Before
	public void setUp() throws Exception {
		context = mock(RibbonCommandContext.class);
		doReturn("GET").when(context).getMethod();
		MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		headers.put("header1", Collections.<String>emptyList());
		headers.put("header2", Arrays.asList("value1", "value2"));
		headers.put("header3", Arrays.asList("value1"));
		doReturn(headers).when(context).getHeaders();
		doReturn(new URI("https://foo")).when(context).uri();
		doReturn("foo").when(context).getServiceId();
		doReturn(new LinkedMultiValueMap<>()).when(context).getParams();
		doReturn("testLoadBalancerKey").when(context).getLoadBalancerKey();
		request = new TestContextAwareRequest(context);
	}

	@After
	public void tearDown() throws Exception {
		context = null;
		request = null;
	}

	@Test
	public void getContext() throws Exception {
		assertThat(request.getContext()).isEqualTo(context);
	}

	@Test
	public void getMethod() throws Exception {
		assertThat(request.getMethod()).isEqualTo(HttpMethod.GET);
	}

	@Test
	public void getURI() throws Exception {
		assertThat(request.getURI()).isEqualTo(new URI("https://foo"));

		RibbonCommandContext badUriContext = mock(RibbonCommandContext.class);
		doReturn(new LinkedMultiValueMap()).when(badUriContext).getHeaders();
		doReturn("foobar").when(badUriContext).getUri();
		ContextAwareRequest badUriRequest = new TestContextAwareRequest(badUriContext);

		assertThat(badUriRequest.getURI()).isNull();

	}

	@Test
	public void getHeaders() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.put("header1", Collections.<String>emptyList());
		headers.put("header2", Arrays.asList("value1", "value2"));
		headers.put("header3", Arrays.asList("value1"));
		assertThat(request.getHeaders()).isEqualTo(headers);
	}

	@Test
	public void getLoadBalancerKey() throws Exception {
		assertThat(request.getLoadBalancerKey()).isEqualTo("testLoadBalancerKey");

		RibbonCommandContext defaultContext = mock(RibbonCommandContext.class);
		doReturn(new LinkedMultiValueMap()).when(defaultContext).getHeaders();
		doReturn(null).when(defaultContext).getLoadBalancerKey();
		ContextAwareRequest defaultRequest = new TestContextAwareRequest(defaultContext);

		assertThat(defaultRequest.getLoadBalancerKey()).isNull();
	}

	static class TestContextAwareRequest extends ContextAwareRequest {

		TestContextAwareRequest(RibbonCommandContext context) {
			super(context);
		}

	}

}
