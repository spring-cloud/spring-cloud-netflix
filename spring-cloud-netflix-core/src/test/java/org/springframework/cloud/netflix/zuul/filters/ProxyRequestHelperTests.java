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

package org.springframework.cloud.netflix.zuul.filters;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.boot.actuate.trace.InMemoryTraceRepository;
import org.springframework.boot.actuate.trace.Trace;
import org.springframework.boot.actuate.trace.TraceRepository;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.netflix.zuul.context.RequestContext;

/**
 * @author Spencer Gibb
 */
public class ProxyRequestHelperTests {

	@Mock
	private TraceRepository traceRepository;

	@Before
	public void init() {
		initMocks(this);
	}

	@Test
	public void debug() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
		request.setContent("{}".getBytes());
		request.addHeader("singleName", "singleValue");
		request.addHeader("multiName", "multiValue1");
		request.addHeader("multiName", "multiValue2");
		RequestContext.getCurrentContext().setRequest(request);

		ProxyRequestHelper helper = new ProxyRequestHelper();
		this.traceRepository = new InMemoryTraceRepository();
		helper.setTraces(this.traceRepository);

		MultiValueMap<String, String> headers = helper.buildZuulRequestHeaders(request);

		helper.debug("POST", "http://example.com", headers,
				new LinkedMultiValueMap<String, String>(), request.getInputStream());
		Trace actual = this.traceRepository.findAll().get(0);
		System.err.println(actual.getInfo());
		assertThat((String)actual.getInfo().get("body"), equalTo("{}"));

	}

	@Test
	public void buildZuulRequestHeadersWork() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.addHeader("singleName", "singleValue");
		request.addHeader("multiName", "multiValue1");
		request.addHeader("multiName", "multiValue2");

		ProxyRequestHelper helper = new ProxyRequestHelper();
		helper.setTraces(this.traceRepository);

		MultiValueMap<String, String> headers = helper.buildZuulRequestHeaders(request);
		List<String> singleName = headers.get("singleName");
		assertThat(singleName, is(notNullValue()));
		assertThat(singleName.size(), is(1));

		List<String> multiName = headers.get("multiName");
		assertThat(multiName, is(notNullValue()));
		assertThat(multiName.size(), is(2));

		List<String> missingName = headers.get("missingName");
		assertThat(missingName, is(nullValue()));

	}

	@Test
	public void buildZuulRequestHeadersRequestsGzipAndOnlyGzip() {
		MockHttpServletRequest request = new MockHttpServletRequest("", "/");

		ProxyRequestHelper helper = new ProxyRequestHelper();

		MultiValueMap<String, String> headers = helper.buildZuulRequestHeaders(request);

		List<String> acceptEncodings = headers.get("accept-encoding");
		assertThat(acceptEncodings, hasSize(1));
		assertThat(acceptEncodings, contains("gzip"));
	}

}
