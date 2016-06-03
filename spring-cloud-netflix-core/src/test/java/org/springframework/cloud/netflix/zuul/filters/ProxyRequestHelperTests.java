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

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.boot.actuate.trace.InMemoryTraceRepository;
import org.springframework.boot.actuate.trace.Trace;
import org.springframework.boot.actuate.trace.TraceRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.netflix.zuul.context.RequestContext;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

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

	@Before
	public void setTestRequestcontext() {
		RequestContext context = new RequestContext();
		RequestContext.testSetCurrentContext(context);
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
		assertThat((String) actual.getInfo().get("body"), equalTo("{}"));

	}

	@Test
	public void shouldDebugBodyDisabled() throws Exception {
		RequestContext context = RequestContext.getCurrentContext();

		ProxyRequestHelper helper = new ProxyRequestHelper();
		helper.setTraceRequestBody(false);

		assertThat("shouldDebugBody wrong", helper.shouldDebugBody(context), is(false));
	}

	@Test
	public void shouldDebugBodyChunked() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
		RequestContext context = RequestContext.getCurrentContext();
		context.setChunkedRequestBody();
		context.setRequest(request);

		ProxyRequestHelper helper = new ProxyRequestHelper();

		assertThat("shouldDebugBody wrong", helper.shouldDebugBody(context), is(false));
	}

	@Test
	public void shouldDebugBodyServlet() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
		RequestContext context = RequestContext.getCurrentContext();
		context.setZuulEngineRan();
		context.setRequest(request);

		ProxyRequestHelper helper = new ProxyRequestHelper();

		assertThat("shouldDebugBody wrong", helper.shouldDebugBody(context), is(false));
	}

	@Test
	public void shouldDebugBodyNullContentType() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
		request.setContentType(null);
		RequestContext context = RequestContext.getCurrentContext();
		context.setRequest(request);

		ProxyRequestHelper helper = new ProxyRequestHelper();

		assertThat("shouldDebugBody wrong", helper.shouldDebugBody(context), is(true));
	}

	@Test
	public void shouldDebugBodyNullRequest() throws Exception {
		RequestContext context = RequestContext.getCurrentContext();

		ProxyRequestHelper helper = new ProxyRequestHelper();

		assertThat("shouldDebugBody wrong", helper.shouldDebugBody(context), is(true));
	}

	@Test
	public void shouldDebugBodyNotMultitypeContentType() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
		request.setContentType(MediaType.APPLICATION_JSON_VALUE);
		RequestContext context = RequestContext.getCurrentContext();
		context.setRequest(request);

		ProxyRequestHelper helper = new ProxyRequestHelper();

		assertThat("shouldDebugBody wrong", helper.shouldDebugBody(context), is(true));
	}

	@Test
	public void shouldDebugBodyMultitypeContentType() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
		request.setContentType(MediaType.MULTIPART_FORM_DATA_VALUE);
		RequestContext context = RequestContext.getCurrentContext();
		context.setRequest(request);

		ProxyRequestHelper helper = new ProxyRequestHelper();

		assertThat("shouldDebugBody wrong", helper.shouldDebugBody(context), is(false));
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

	@Test
	public void setResponseLowercase() throws IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
		MockHttpServletResponse response = new MockHttpServletResponse();

		RequestContext context = RequestContext.getCurrentContext();
		context.setRequest(request);
		context.setResponse(response);

		ProxyRequestHelper helper = new ProxyRequestHelper();

		MultiValueMap<String, String> headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_ENCODING.toLowerCase(), "gzip");

		helper.setResponse(200, request.getInputStream(), headers);
		assertTrue(context.getResponseGZipped());
	}

	@Test
	public void setResponseUppercase() throws IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
		MockHttpServletResponse response = new MockHttpServletResponse();

		RequestContext context = RequestContext.getCurrentContext();
		context.setRequest(request);
		context.setResponse(response);

		ProxyRequestHelper helper = new ProxyRequestHelper();

		MultiValueMap<String, String> headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_ENCODING, "gzip");

		helper.setResponse(200, request.getInputStream(), headers);
		assertTrue(context.getResponseGZipped());
	}

	@Test
	public void getQueryString() {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("a", "1234");
		params.add("b", "5678");

		String queryString = new ProxyRequestHelper().getQueryString(params);

		assertThat(queryString, is("?a=1234&b=5678"));
	}

	@Test
	public void getQueryStringWithEmptyParam() {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("wsdl", "");

		String queryString = new ProxyRequestHelper().getQueryString(params);

		assertThat(queryString, is("?wsdl"));
	}

	@Test
	public void buildZuulRequestURIWithUTF8() throws Exception {
		String encodedURI = "/resource/esp%C3%A9cial-char";
		String decodedURI = "/resource/espécial-char";

		MockHttpServletRequest request = new MockHttpServletRequest("GET", encodedURI);
		request.setCharacterEncoding("UTF-8");
		final RequestContext context = RequestContext.getCurrentContext();
		context.setRequest(request);
		context.set("requestURI", decodedURI);

		final String requestURI = new ProxyRequestHelper().buildZuulRequestURI(request);
		assertThat(requestURI, equalTo(encodedURI));
	}

	@Test
	public void buildZuulRequestURIWithDefaultEncoding() {
		String encodedURI = "/resource/esp%E9cial-char";
		String decodedURI = "/resource/espécial-char";

		MockHttpServletRequest request = new MockHttpServletRequest("GET", encodedURI);
		final RequestContext context = RequestContext.getCurrentContext();
		context.setRequest(request);
		context.set("requestURI", decodedURI);

		final String requestURI = new ProxyRequestHelper().buildZuulRequestURI(request);
		assertThat(requestURI, equalTo(encodedURI));
	}

}
