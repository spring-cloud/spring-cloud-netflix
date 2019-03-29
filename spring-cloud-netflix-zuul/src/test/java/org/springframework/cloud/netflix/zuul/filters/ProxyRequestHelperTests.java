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

package org.springframework.cloud.netflix.zuul.filters;

import java.io.IOException;
import java.util.List;

import com.netflix.util.Pair;
import com.netflix.zuul.context.RequestContext;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import org.springframework.boot.actuate.trace.http.HttpTrace;
import org.springframework.boot.actuate.trace.http.HttpTraceRepository;
import org.springframework.boot.actuate.trace.http.InMemoryHttpTraceRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.REQUEST_URI_KEY;

/**
 * @author Spencer Gibb
 */
public class ProxyRequestHelperTests {

	@Mock
	private HttpTraceRepository traceRepository;

	@Before
	public void init() {
		initMocks(this);
	}

	@Before
	public void setTestRequestcontext() {
		RequestContext context = new RequestContext();
		RequestContext.testSetCurrentContext(context);
	}

	@After
	public void clear() {
		RequestContext.getCurrentContext().clear();
	}

	@Test
	public void debug() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
		request.setContent("{}".getBytes());
		request.addHeader("singleName", "singleValue");
		request.addHeader("multiName", "multiValue1");
		request.addHeader("multiName", "multiValue2");
		RequestContext.getCurrentContext().setRequest(request);

		TraceProxyRequestHelper helper = new TraceProxyRequestHelper(
				new ZuulProperties());
		this.traceRepository = new InMemoryHttpTraceRepository();
		helper.setTraces(this.traceRepository);

		MultiValueMap<String, String> headers = helper.buildZuulRequestHeaders(request);

		helper.debug("POST", "https://example.com", headers, new LinkedMultiValueMap<>(),
				request.getInputStream());
		HttpTrace actual = this.traceRepository.findAll().get(0);
		Assertions.assertThat(actual.getRequest().getHeaders()).containsKeys("singleName",
				"multiName");
	}

	@Test
	public void shouldDebugBodyDisabled() throws Exception {
		RequestContext context = RequestContext.getCurrentContext();

		ZuulProperties zuulProperties = new ZuulProperties();
		zuulProperties.setTraceRequestBody(false);
		ProxyRequestHelper helper = new ProxyRequestHelper(zuulProperties);

		assertThat(helper.shouldDebugBody(context)).as("shouldDebugBody wrong").isFalse();
	}

	@Test
	public void shouldDebugBodyChunked() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
		RequestContext context = RequestContext.getCurrentContext();
		context.setChunkedRequestBody();
		context.setRequest(request);

		ProxyRequestHelper helper = new ProxyRequestHelper(new ZuulProperties());

		assertThat(helper.shouldDebugBody(context)).as("shouldDebugBody wrong").isFalse();
	}

	@Test
	public void shouldDebugBodyServlet() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
		RequestContext context = RequestContext.getCurrentContext();
		context.setZuulEngineRan();
		context.setRequest(request);

		ProxyRequestHelper helper = new ProxyRequestHelper(new ZuulProperties());

		assertThat(helper.shouldDebugBody(context)).as("shouldDebugBody wrong").isFalse();
	}

	@Test
	public void shouldDebugBodyNullContentType() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
		request.setContentType(null);
		RequestContext context = RequestContext.getCurrentContext();
		context.setRequest(request);

		ZuulProperties zuulProperties = new ZuulProperties();
		zuulProperties.setTraceRequestBody(true);
		ProxyRequestHelper helper = new ProxyRequestHelper(zuulProperties);

		assertThat(helper.shouldDebugBody(context)).as("shouldDebugBody wrong").isTrue();
	}

	@Test
	public void shouldDebugBodyNullRequest() throws Exception {
		RequestContext context = RequestContext.getCurrentContext();

		ZuulProperties zuulProperties = new ZuulProperties();
		zuulProperties.setTraceRequestBody(true);
		ProxyRequestHelper helper = new ProxyRequestHelper(zuulProperties);

		assertThat(helper.shouldDebugBody(context)).as("shouldDebugBody wrong").isTrue();
	}

	@Test
	public void shouldDebugBodyNotMultitypeContentType() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
		request.setContentType(MediaType.APPLICATION_JSON_VALUE);
		RequestContext context = RequestContext.getCurrentContext();
		context.setRequest(request);

		ZuulProperties zuulProperties = new ZuulProperties();
		zuulProperties.setTraceRequestBody(true);
		ProxyRequestHelper helper = new ProxyRequestHelper(zuulProperties);

		assertThat(helper.shouldDebugBody(context)).as("shouldDebugBody wrong").isTrue();
	}

	@Test
	public void shouldDebugBodyMultitypeContentType() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
		request.setContentType(MediaType.MULTIPART_FORM_DATA_VALUE);
		RequestContext context = RequestContext.getCurrentContext();
		context.setRequest(request);

		ProxyRequestHelper helper = new ProxyRequestHelper(new ZuulProperties());

		assertThat(helper.shouldDebugBody(context)).as("shouldDebugBody wrong").isFalse();
	}

	@Test
	public void buildZuulRequestHeadersWork() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.addHeader("singleName", "singleValue");
		request.addHeader("multiName", "multiValue1");
		request.addHeader("multiName", "multiValue2");

		TraceProxyRequestHelper helper = new TraceProxyRequestHelper(
				new ZuulProperties());
		helper.setTraces(this.traceRepository);

		MultiValueMap<String, String> headers = helper.buildZuulRequestHeaders(request);
		List<String> singleName = headers.get("singleName");
		assertThat(singleName).isNotNull();
		assertThat(singleName.size()).isEqualTo(1);

		List<String> multiName = headers.get("multiName");
		assertThat(multiName).isNotNull();
		assertThat(multiName.size()).isEqualTo(2);

		List<String> missingName = headers.get("missingName");
		assertThat(missingName).isNull();

	}

	@Test
	public void buildZuulRequestHeadersRequestsGzipAndOnlyGzip() {
		MockHttpServletRequest request = new MockHttpServletRequest("", "/");

		ProxyRequestHelper helper = new ProxyRequestHelper(new ZuulProperties());

		MultiValueMap<String, String> headers = helper.buildZuulRequestHeaders(request);

		List<String> acceptEncodings = headers.get("accept-encoding");
		assertThat(acceptEncodings).hasSize(1);
		assertThat(acceptEncodings).containsExactly("gzip");
	}

	@Test
	public void buildZuulRequestHeadersRequestsContentEncoding() {
		MockHttpServletRequest request = new MockHttpServletRequest("", "/");
		request.addHeader("content-encoding", "identity");

		ProxyRequestHelper helper = new ProxyRequestHelper(new ZuulProperties());

		MultiValueMap<String, String> headers = helper.buildZuulRequestHeaders(request);

		List<String> contentEncodings = headers.get("content-encoding");
		assertThat(contentEncodings).hasSize(1);
		assertThat(contentEncodings).containsExactly("identity");
	}

	@Test
	public void buildZuulRequestHeadersRequestsAcceptEncoding() {
		MockHttpServletRequest request = new MockHttpServletRequest("", "/");
		request.addHeader("accept-encoding", "identity");

		ProxyRequestHelper helper = new ProxyRequestHelper(new ZuulProperties());

		MultiValueMap<String, String> headers = helper.buildZuulRequestHeaders(request);

		List<String> acceptEncodings = headers.get("accept-encoding");
		assertThat(acceptEncodings).hasSize(1);
		assertThat(acceptEncodings).containsExactly("identity");
	}

	@Test
	public void addHostHeader() {
		MockHttpServletRequest request = new MockHttpServletRequest("", "/");
		request.addHeader("host", "foo.com");

		ZuulProperties zuulProperties = new ZuulProperties();
		zuulProperties.setAddHostHeader(true);
		ProxyRequestHelper helper = new ProxyRequestHelper(zuulProperties);

		MultiValueMap<String, String> headers = helper.buildZuulRequestHeaders(request);

		List<String> acceptEncodings = headers.get("host");
		assertThat(acceptEncodings).hasSize(1);
		assertThat(acceptEncodings).containsExactly("foo.com");

		zuulProperties.setAddHostHeader(false);
		helper = new ProxyRequestHelper(zuulProperties);
		headers = helper.buildZuulRequestHeaders(request);

		acceptEncodings = headers.get("host");
		assertThat(acceptEncodings).isNull();
	}

	@Test
	public void setResponseLowercase() throws IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
		MockHttpServletResponse response = new MockHttpServletResponse();

		RequestContext context = RequestContext.getCurrentContext();
		context.setRequest(request);
		context.setResponse(response);

		ProxyRequestHelper helper = new ProxyRequestHelper(new ZuulProperties());

		MultiValueMap<String, String> headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_ENCODING.toLowerCase(), "gzip");

		helper.setResponse(200, request.getInputStream(), headers);
		assertThat(context.getResponseGZipped()).isTrue();
	}

	@Test
	public void setResponseShouldSetOriginResponseHeaders() throws IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
		MockHttpServletResponse response = new MockHttpServletResponse();

		RequestContext context = RequestContext.getCurrentContext();
		context.setRequest(request);
		context.setResponse(response);

		ProxyRequestHelper helper = new ProxyRequestHelper(new ZuulProperties());

		MultiValueMap<String, String> headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "text/plain");
		headers.add("some-header", "some-value");

		helper.setResponse(200, request.getInputStream(), headers);
		assertThat(context.getOriginResponseHeaders()).contains(
				new Pair<>(HttpHeaders.CONTENT_TYPE, "text/plain"),
				new Pair<>("some-header", "some-value"));
	}

	@Test
	public void setResponseUppercase() throws IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
		MockHttpServletResponse response = new MockHttpServletResponse();

		RequestContext context = RequestContext.getCurrentContext();
		context.setRequest(request);
		context.setResponse(response);

		ProxyRequestHelper helper = new ProxyRequestHelper(new ZuulProperties());

		MultiValueMap<String, String> headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_ENCODING, "gzip");

		helper.setResponse(200, request.getInputStream(), headers);
		assertThat(context.getResponseGZipped()).isTrue();
	}

	@Test
	public void getQueryString() {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("a", "1234");
		params.add("b", "5678");

		String queryString = new ProxyRequestHelper(new ZuulProperties())
				.getQueryString(params);

		assertThat(queryString).isEqualTo("?a=1234&b=5678");
	}

	@Test
	public void getQueryStringWithEmptyParam() {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("wsdl", "");

		String queryString = new ProxyRequestHelper(new ZuulProperties())
				.getQueryString(params);

		assertThat(queryString).isEqualTo("?wsdl");
	}

	@Test
	public void getQueryStringEncoded() {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("foo", "weird#chars");

		String queryString = new ProxyRequestHelper(new ZuulProperties())
				.getQueryString(params);

		assertThat(queryString).isEqualTo("?foo=weird%23chars");
	}

	@Test
	public void getQueryParamNameWithColon() {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("foo:bar", "baz");
		params.add("foobar", "bam");
		params.add("foo\fbar", "bat"); // form feed is the colon replacement char

		String queryString = new ProxyRequestHelper(new ZuulProperties())
				.getQueryString(params);

		assertThat(queryString).isEqualTo("?foo:bar=baz&foobar=bam&foo%0Cbar=bat");
	}

	@Test
	public void buildZuulRequestURIWithUTF8() throws Exception {
		String encodedURI = "/resource/esp%C3%A9cial-char";
		String decodedURI = "/resource/espécial-char";

		MockHttpServletRequest request = new MockHttpServletRequest("GET", encodedURI);
		request.setCharacterEncoding("UTF-8");
		final RequestContext context = RequestContext.getCurrentContext();
		context.setRequest(request);
		context.set(REQUEST_URI_KEY, decodedURI);

		final String requestURI = new ProxyRequestHelper(new ZuulProperties())
				.buildZuulRequestURI(request);
		assertThat(requestURI).isEqualTo(encodedURI);
	}

	@Test
	public void buildZuulRequestURIWithDefaultEncoding() {
		String encodedURI = "/resource/esp%E9cial-char";
		String decodedURI = "/resource/espécial-char";

		MockHttpServletRequest request = new MockHttpServletRequest("GET", encodedURI);
		final RequestContext context = RequestContext.getCurrentContext();
		context.setRequest(request);
		context.set(REQUEST_URI_KEY, decodedURI);

		final String requestURI = new ProxyRequestHelper(new ZuulProperties())
				.buildZuulRequestURI(request);
		assertThat(requestURI).isEqualTo(encodedURI);
	}

	@Test
	public void getUTF8Url() {
		String requestURI = "/oléדרעק";
		String encodedRequestURI = "/ol%C3%A9%D7%93%D7%A8%D7%A2%D7%A7";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestURI);
		request.setCharacterEncoding("UTF-8");

		RequestContext context = RequestContext.getCurrentContext();
		context.set(REQUEST_URI_KEY, requestURI);

		ProxyRequestHelper helper = new ProxyRequestHelper(new ZuulProperties());

		String uri = helper.buildZuulRequestURI(request);

		assertThat(uri).isEqualTo(encodedRequestURI);
	}

	@Test
	public void getDefaultEncodingUrl() {
		String requestURI = "/oléדרעק";
		String encodedRequestURI = "/ol%E9%3F%3F%3F%3F";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestURI);

		RequestContext context = RequestContext.getCurrentContext();
		context.set(REQUEST_URI_KEY, requestURI);

		ProxyRequestHelper helper = new ProxyRequestHelper(new ZuulProperties());

		String uri = helper.buildZuulRequestURI(request);

		assertThat(uri).isEqualTo(encodedRequestURI);
	}

}
