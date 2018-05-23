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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.monitoring.CounterFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.Configurable;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientConnectionManagerFactory;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientFactory;
import org.springframework.cloud.commons.httpclient.DefaultApacheHttpClientConnectionManagerFactory;
import org.springframework.cloud.commons.httpclient.DefaultApacheHttpClientFactory;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.metrics.EmptyCounterFactory;
import org.springframework.cloud.netflix.zuul.util.ZuulRuntimeException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.boot.test.util.EnvironmentTestUtils.addEnvironment;
import static org.springframework.util.StreamUtils.copyToByteArray;
import static org.springframework.util.StreamUtils.copyToString;

/**
 * @author Andreas Kluth
 * @author Spencer Gibb
 * @author Gang Li
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SimpleHostRoutingFilterTests.SampleApplication.class,
		webEnvironment = RANDOM_PORT,
		properties = {"server.servlet.contextPath: /app"})
@DirtiesContext
public class SimpleHostRoutingFilterTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@LocalServerPort
	private int port;

	@Before
	public void setup() {
		CounterFactory.initialize(new EmptyCounterFactory());
		RequestContext.testSetCurrentContext(new RequestContext());
	}

	@After
	public void clear() {
		if (this.context != null) {
			this.context.close();
		}
		CounterFactory.initialize(null);

		RequestContext.testSetCurrentContext(null);
		RequestContext.getCurrentContext().clear();
	}

	@Test
	public void timeoutPropertiesAreApplied() {
		addEnvironment(this.context, "zuul.host.socket-timeout-millis=11000",
				"zuul.host.connect-timeout-millis=2100", "zuul.host.connection-request-timeout-millis=2500");
		setupContext();
		CloseableHttpClient httpClient = getFilter().newClient();
		Assertions.assertThat(httpClient).isInstanceOf(Configurable.class);
		RequestConfig config = ((Configurable) httpClient).getConfig();
		assertEquals(11000, config.getSocketTimeout());
		assertEquals(2100, config.getConnectTimeout());
		assertEquals(2500, config.getConnectionRequestTimeout());
	}

	@Test
	public void connectionPropertiesAreApplied() {
		addEnvironment(this.context, "zuul.host.maxTotalConnections=100",
				"zuul.host.maxPerRouteConnections=10", "zuul.host.timeToLive=5",
				"zuul.host.timeUnit=SECONDS");
		setupContext();
		PoolingHttpClientConnectionManager connMgr = (PoolingHttpClientConnectionManager)getFilter().getConnectionManager();
		assertEquals(100, connMgr.getMaxTotal());
		assertEquals(10, connMgr.getDefaultMaxPerRoute());
		Object pool = getField(connMgr, "pool");
		Long timeToLive = getField(pool, "timeToLive");
		TimeUnit timeUnit = getField(pool, "tunit");
		assertEquals(new Long(5), timeToLive);
		assertEquals(TimeUnit.SECONDS, timeUnit);
	}

	protected <T> T getField(Object target, String name) {
		Field field = ReflectionUtils.findField(target.getClass(), name);
		ReflectionUtils.makeAccessible(field);
		Object value = ReflectionUtils.getField(field, target);
		return (T)value;
	}

	@Test
	public void validateSslHostnamesByDefault() {
		setupContext();
		assertTrue("Hostname verification should be enabled by default",
				getFilter().isSslHostnameValidationEnabled());
	}

	@Test
	public void validationOfSslHostnamesCanBeDisabledViaProperty() {
		addEnvironment(this.context, "zuul.sslHostnameValidationEnabled=false");
		setupContext();
		assertFalse("Hostname verification should be disabled via property",
				getFilter().isSslHostnameValidationEnabled());
	}

	@Test
	public void defaultPropertiesAreApplied() {
		setupContext();
		PoolingHttpClientConnectionManager connMgr = (PoolingHttpClientConnectionManager)getFilter().getConnectionManager();

		assertEquals(200, connMgr.getMaxTotal());
		assertEquals(20, connMgr.getDefaultMaxPerRoute());
	}

	@Test
	public void deleteRequestBuiltWithBody() {
		setupContext();
		InputStreamEntity inputStreamEntity = new InputStreamEntity(new ByteArrayInputStream(new byte[]{1}));
		HttpRequest httpRequest = getFilter().buildHttpRequest("DELETE", "uri", inputStreamEntity,
				new LinkedMultiValueMap<>(), new LinkedMultiValueMap<>(), new MockHttpServletRequest());

		assertTrue(httpRequest instanceof HttpEntityEnclosingRequest);
		HttpEntityEnclosingRequest httpEntityEnclosingRequest = (HttpEntityEnclosingRequest) httpRequest;
		assertTrue(httpEntityEnclosingRequest.getEntity() != null);
	}

	@Test
	public void httpClientDoesNotDecompressEncodedData() throws Exception {
		setupContext();
		InputStreamEntity inputStreamEntity = new InputStreamEntity(new ByteArrayInputStream(new byte[]{1}));
		HttpRequest httpRequest = getFilter().buildHttpRequest("GET", "/app/compressed/get/1", inputStreamEntity,
				new LinkedMultiValueMap<>(), new LinkedMultiValueMap<>(), new MockHttpServletRequest());

		CloseableHttpResponse response = getFilter().newClient().execute(new HttpHost("localhost", this.port), httpRequest);
		assertEquals(200, response.getStatusLine().getStatusCode());
		byte[] responseBytes = copyToByteArray(response.getEntity().getContent());
		assertTrue(Arrays.equals(GZIPCompression.compress("Get 1"), responseBytes));
	}

	@Test
	public void httpClientPreservesUnencodedData() throws Exception {
		setupContext();
		InputStreamEntity inputStreamEntity = new InputStreamEntity(new ByteArrayInputStream(new byte[]{1}));
		HttpRequest httpRequest = getFilter().buildHttpRequest("GET", "/app/get/1", inputStreamEntity,
				new LinkedMultiValueMap<>(), new LinkedMultiValueMap<>(), new MockHttpServletRequest());

		CloseableHttpResponse response = getFilter().newClient().execute(new HttpHost("localhost", this.port), httpRequest);
		assertEquals(200, response.getStatusLine().getStatusCode());
		String responseString = copyToString(response.getEntity().getContent(), Charset.forName("UTF-8"));
		assertTrue("Get 1".equals(responseString));
	}


	@Test
	public void redirectTest() throws IOException {
		setupContext();
		InputStreamEntity inputStreamEntity = new InputStreamEntity(new ByteArrayInputStream(new byte[]{}));
		HttpRequest httpRequest = getFilter().buildHttpRequest("GET", "/app/redirect", inputStreamEntity,
				new LinkedMultiValueMap<>(), new LinkedMultiValueMap<>(), new MockHttpServletRequest());

		CloseableHttpResponse response = getFilter().newClient().execute(new HttpHost("localhost", this.port), httpRequest);
		assertEquals(302, response.getStatusLine().getStatusCode());
		String responseString = copyToString(response.getEntity().getContent(), Charset.forName("UTF-8"));
		assertTrue(response.getLastHeader("Location").getValue().contains("/app/get/5"));
	}

	@Test
	public void contentLengthNegativeTest() throws IOException {
		contentLengthTest(-1000L);
	}

	@Test
	public void contentLengthNegativeOneTest() throws IOException {
		contentLengthTest(-1L);
	}

	@Test
	public void contentLengthZeroTest() throws IOException {
		contentLengthTest(0L);
	}

	@Test
	public void contentLengthOneTest() throws IOException {
		contentLengthTest(1L);
	}

	@Test
	public void contentLength1KbTest() throws IOException {
		contentLengthTest(1000L);
	}

	@Test
	public void contentLength1MbTest() throws IOException {
		contentLengthTest(1000000L);
	}

	@Test
	public void contentLength1GbTest() throws IOException {
		contentLengthTest(1000000000L);
	}

	@Test
	public void contentLength2GbTest() throws IOException {
		contentLengthTest(2000000000L);
	}

	@Test
	public void contentLength3GbTest() throws IOException {
		contentLengthTest(3000000000L);
	}

	@Test
	public void contentLength4GbTest() throws IOException {
		contentLengthTest(4000000000L);
	}

	@Test
	public void contentLength5GbTest() throws IOException {
		contentLengthTest(5000000000L);
	}

	@Test
	public void contentLength6GbTest() throws IOException {
		contentLengthTest(6000000000L);
	}

	@Test
	public void contentLengthServlet30WithHeaderNegativeTest() throws IOException {
		contentLengthServlet30WithHeaderTest(-1000L);
	}

	@Test
	public void contentLengthServlet30WithHeaderNegativeOneTest() throws IOException {
		contentLengthServlet30WithHeaderTest(-1L);
	}

	@Test
	public void contentLengthServlet30WithHeaderZeroTest() throws IOException {
		contentLengthServlet30WithHeaderTest(0L);
	}

	@Test
	public void contentLengthServlet30WithHeaderOneTest() throws IOException {
		contentLengthServlet30WithHeaderTest(1L);
	}

	@Test
	public void contentLengthServlet30WithHeader1KbTest() throws IOException {
		contentLengthServlet30WithHeaderTest(1000L);
	}

	@Test
	public void contentLengthServlet30WithHeader1MbTest() throws IOException {
		contentLengthServlet30WithHeaderTest(1000000L);
	}

	@Test
	public void contentLengthServlet30WithHeader1GbTest() throws IOException {
		contentLengthServlet30WithHeaderTest(1000000000L);
	}

	@Test
	public void contentLengthServlet30WithHeader2GbTest() throws IOException {
		contentLengthServlet30WithHeaderTest(2000000000L);
	}

	@Test
	public void contentLengthServlet30WithHeader3GbTest() throws IOException {
		contentLengthServlet30WithHeaderTest(3000000000L);
	}

	@Test
	public void contentLengthServlet30WithHeader4GbTest() throws IOException {
		contentLengthServlet30WithHeaderTest(4000000000L);
	}

	@Test
	public void contentLengthServlet30WithHeader5GbTest() throws IOException {
		contentLengthServlet30WithHeaderTest(5000000000L);
	}

	@Test
	public void contentLengthServlet30WithHeader6GbTest() throws IOException {
		contentLengthServlet30WithHeaderTest(6000000000L);
	}

	@Test
	public void contentLengthServlet30WithInvalidLongHeaderTest() throws IOException {
		setupContext();
		MockMultipartHttpServletRequest request = getMockedReqest(-1L);
		request.addHeader(HttpHeaders.CONTENT_LENGTH, "InvalidLong");
		contentLengthTest(-1L, getServlet30Filter(), request);
	}

	@Test
	public void contentLengthServlet30WithoutHeaderNegativeTest() throws IOException {
		contentLengthServlet30WithoutHeaderTest(-1000L);
	}

	@Test
	public void contentLengthServlet30WithoutHeaderNegativeOneTest() throws IOException {
		contentLengthServlet30WithoutHeaderTest(-1L);
	}

	@Test
	public void contentLengthServlet30WithoutHeaderZeroTest() throws IOException {
		contentLengthServlet30WithoutHeaderTest(0L);
	}

	@Test
	public void contentLengthServlet30WithoutHeaderOneTest() throws IOException {
		contentLengthServlet30WithoutHeaderTest(1L);
	}

	@Test
	public void contentLengthServlet30WithoutHeader1KbTest() throws IOException {
		contentLengthServlet30WithoutHeaderTest(1000L);
	}

	@Test
	public void contentLengthServlet30WithoutHeader1MbTest() throws IOException {
		contentLengthServlet30WithoutHeaderTest(1000000L);
	}

	@Test
	public void contentLengthServlet30WithoutHeader1GbTest() throws IOException {
		contentLengthServlet30WithoutHeaderTest(1000000000L);
	}

	@Test
	public void contentLengthServlet30WithoutHeader2GbTest() throws IOException {
		contentLengthServlet30WithoutHeaderTest(2000000000L);
	}

	@Test
	public void contentLengthServlet30WithoutHeader3GbTest() throws IOException {
		contentLengthServlet30WithoutHeaderTest(3000000000L);
	}

	@Test
	public void contentLengthServlet30WithoutHeader4GbTest() throws IOException {
		contentLengthServlet30WithoutHeaderTest(4000000000L);
	}

	@Test
	public void contentLengthServlet30WithoutHeader5GbTest() throws IOException {
		contentLengthServlet30WithoutHeaderTest(5000000000L);
	}

	@Test
	public void contentLengthServlet30WithoutHeader6GbTest() throws IOException {
		contentLengthServlet30WithoutHeaderTest(6000000000L);
	}

	public void contentLengthTest(Long contentLength) throws IOException {
		setupContext();

		contentLengthTest(contentLength, getFilter(), getMockedReqest(contentLength));
	}

	public void contentLengthServlet30WithHeaderTest(Long contentLength) throws IOException {
		setupContext();
		MockMultipartHttpServletRequest request = getMockedReqest(contentLength);
		request.addHeader(HttpHeaders.CONTENT_LENGTH, contentLength);
		contentLengthTest(contentLength, getServlet30Filter(), request);
	}

	public void contentLengthServlet30WithoutHeaderTest(Long contentLength) throws IOException {
		setupContext();

		//Although contentLength.intValue is not always equals to contentLength, that's the expected result when calling
		// request.getContentLength() from servlet 3.0 implementation.
		contentLengthTest(Long.parseLong("" + contentLength.intValue()), getServlet30Filter(), getMockedReqest(contentLength));
	}

	public MockMultipartHttpServletRequest getMockedReqest(final Long contentLength) throws IOException {

		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest() {
			@Override
			public int getContentLength() {
				return contentLength.intValue();
			}

			@Override
			public long getContentLengthLong() {
				return contentLength;
			}
		};

		return request;
	}

	public void contentLengthTest(Long expectedContentLength, SimpleHostRoutingFilter filter, MockMultipartHttpServletRequest request) throws IOException {
		byte[] data = "poprqwueproqiwuerpoqweiurpo".getBytes();
		MockMultipartFile file = new MockMultipartFile("test.zip", "test.zip",
				"application/zip", data);
		String boundary = "q1w2e3r4t5y6u7i8o9";
		request.setContentType("multipart/form-data; boundary=" + boundary);
		request.setContent(
				createFileContent(data, boundary, "application/zip", "test.zip"));
		request.addFile(file);
		request.setMethod("POST");
		request.setParameter("variant", "php");
		request.setParameter("os", "mac");
		request.setParameter("version", "3.4");
		request.setRequestURI("/app/echo");

		MockHttpServletResponse response = new MockHttpServletResponse();
		RequestContext.getCurrentContext().setRequest(request);
		RequestContext.getCurrentContext().setResponse(new MockHttpServletResponse());
		URL url = new URL("http://localhost:" + this.port);
		RequestContext.getCurrentContext().set("routeHost", url);
		filter.run();

		String responseString = IOUtils.toString(new GZIPInputStream(
				((CloseableHttpResponse) RequestContext.getCurrentContext()
						.get("zuulResponse")).getEntity().getContent()));
		assertTrue(!responseString.isEmpty());
		if (expectedContentLength < 0) {
			assertThat(responseString, containsString("\""
					+ HttpHeaders.TRANSFER_ENCODING.toLowerCase() + "\":\"chunked\""));
			assertThat(responseString,
					not(containsString(HttpHeaders.CONTENT_LENGTH.toLowerCase())));
		}
		else {
			assertThat(responseString,
					containsString("\"" + HttpHeaders.CONTENT_LENGTH.toLowerCase()
							+ "\":\"" + expectedContentLength + "\""));
		}
	}

	public byte[] createFileContent(byte[] data, String boundary, String contentType,
			String fileName) {
		String start = "--" + boundary
				+ "\r\n Content-Disposition: form-data; name=\"file\"; filename=\""
				+ fileName + "\"\r\n" + "Content-type: " + contentType + "\r\n\r\n";
		;

		String end = "\r\n--" + boundary + "--"; // correction suggested @butfly
		return ArrayUtils.addAll(start.getBytes(),
				ArrayUtils.addAll(data, end.getBytes()));
	}

	@Test
	public void zuulHostKeysUpdateHttpClient() {
		setupContext();
		SimpleHostRoutingFilter filter = getFilter();
		CloseableHttpClient httpClient = (CloseableHttpClient) ReflectionTestUtils.getField(filter, "httpClient");
		EnvironmentChangeEvent event = new EnvironmentChangeEvent(Collections.singleton("zuul.host.mykey"));
		filter.onPropertyChange(event);
		CloseableHttpClient newhttpClient = (CloseableHttpClient) ReflectionTestUtils.getField(filter, "httpClient");
		Assertions.assertThat(httpClient).isNotEqualTo(newhttpClient);
	}

	@Test
	public void getRequestBody() throws Exception {
		setupContext();
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
		request.setContent("{1}".getBytes());
		request.addHeader("singleName", "singleValue");
		request.addHeader("multiName", "multiValue1");
		request.addHeader("multiName", "multiValue2");
		RequestContext.getCurrentContext().setRequest(request);
		InputStream inputStream = getFilter().getRequestBody(request);
		assertTrue(Arrays.equals("{1}".getBytes(), copyToByteArray(inputStream)));
	}

	@Test
	public void putRequestBuiltWithBody() {
		setupContext();
		InputStreamEntity inputStreamEntity = new InputStreamEntity(new ByteArrayInputStream(new byte[]{1}));
		HttpRequest httpRequest = getFilter().buildHttpRequest("PUT", "uri", inputStreamEntity,
			new LinkedMultiValueMap<>(), new LinkedMultiValueMap<>(), new MockHttpServletRequest());

		assertTrue(httpRequest instanceof HttpEntityEnclosingRequest);
		HttpEntityEnclosingRequest httpEntityEnclosingRequest = (HttpEntityEnclosingRequest) httpRequest;
		assertTrue(httpEntityEnclosingRequest.getEntity() != null);
	}

	@Test
	public void postRequestBuiltWithBody() {
		setupContext();
		InputStreamEntity inputStreamEntity = new InputStreamEntity(new ByteArrayInputStream(new byte[]{1}));
		HttpRequest httpRequest = getFilter().buildHttpRequest("POST", "uri", inputStreamEntity,
			new LinkedMultiValueMap<>(), new LinkedMultiValueMap<>(), new MockHttpServletRequest());

		assertTrue(httpRequest instanceof HttpEntityEnclosingRequest);
		HttpEntityEnclosingRequest httpEntityEnclosingRequest = (HttpEntityEnclosingRequest) httpRequest;
		assertTrue(httpEntityEnclosingRequest.getEntity() != null);
	}

	@Test
	public void pathRequestBuiltWithBody() {
		setupContext();
		InputStreamEntity inputStreamEntity = new InputStreamEntity(new ByteArrayInputStream(new byte[]{1}));
		HttpRequest httpRequest = getFilter().buildHttpRequest("PATCH", "uri", inputStreamEntity,
			new LinkedMultiValueMap<>(), new LinkedMultiValueMap<>(), new MockHttpServletRequest());

		HttpPatch basicHttpRequest = (HttpPatch) httpRequest;
		assertTrue(basicHttpRequest.getEntity() != null);
	}

	@Test
	public void shouldFilterFalse() {
		setupContext();
		assertEquals(false, getFilter().shouldFilter());
	}

	@Test
	public void shouldFilterTrue() throws Exception {
		setupContext();
		RequestContext.getCurrentContext().set("routeHost", new URL("http://localhost:8080"));
		RequestContext.getCurrentContext().set("sendZuulResponse", true);
		assertEquals(true, getFilter().shouldFilter());
	}

	@Test
	public void filterOrder() {
		setupContext();
		assertEquals(100, getFilter().filterOrder());
	}

	@Test(expected = ZuulRuntimeException.class)
	public void run() throws Exception {
		setupContext();
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
		request.setContent("{1}".getBytes());
		request.addHeader("singleName", "singleValue");
		request.addHeader("multiName", "multiValue1");
		request.addHeader("multiName", "multiValue2");
		RequestContext.getCurrentContext().setRequest(request);
		URL url = new URL("http://localhost:8080");
		RequestContext.getCurrentContext().set("routeHost", url);
		getFilter().run();
	}

	private void setupContext() {
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				TestConfiguration.class);
		this.context.refresh();
	}

	private SimpleHostRoutingFilter getFilter() {
		return this.context.getBean(SimpleHostRoutingFilter.class);
	}

	private SimpleHostRoutingFilter getServlet30Filter() {
		SimpleHostRoutingFilter filter = getFilter();
		filter.setUseServlet31(false);
		return filter;
	}

	@Configuration
	@EnableConfigurationProperties
	protected static class TestConfiguration {
		@Bean
		ZuulProperties zuulProperties() {
			return new ZuulProperties();
		}

		@Bean
		ApacheHttpClientFactory clientFactory() {return new DefaultApacheHttpClientFactory(HttpClientBuilder.create()); }

		@Bean
		ApacheHttpClientConnectionManagerFactory connectionManagerFactory() { return new DefaultApacheHttpClientConnectionManagerFactory(); }

		@Bean
		SimpleHostRoutingFilter simpleHostRoutingFilter(ZuulProperties zuulProperties,
														ApacheHttpClientConnectionManagerFactory connectionManagerFactory,
														ApacheHttpClientFactory clientFactory) {
			return new SimpleHostRoutingFilter(new ProxyRequestHelper(), zuulProperties, connectionManagerFactory, clientFactory);
		}
	}

    @Configuration
    @EnableAutoConfiguration
    @RestController
    static class SampleApplication {

        @RequestMapping(value = "/compressed/get/{id}", method = RequestMethod.GET)
        public byte[] getCompressed(@PathVariable String id, HttpServletResponse response) throws IOException {
            response.setHeader("content-encoding", "gzip");
            return GZIPCompression.compress("Get " + id);
        }

        @RequestMapping(value = "/get/{id}", method = RequestMethod.GET)
        public String getString(@PathVariable String id, HttpServletResponse response) throws IOException {
            return "Get " + id;
        }

        @RequestMapping(value = "/redirect", method = RequestMethod.GET)
        public String redirect(HttpServletResponse response) throws IOException {
            response.sendRedirect("/app/get/5");
            return null;
        }

		@RequestMapping(value = "/echo")
		public Map<String, Object> echoRequestAttributes(@RequestHeader HttpHeaders httpHeaders, HttpServletRequest request) throws IOException {
			Map<String, Object> result = new HashMap<>();
			result.put("headers", httpHeaders.toSingleValueMap());

			return result;
		}

		@Bean
		MultipartConfigElement multipartConfigElement() {
			long maxSize = 10l * 1024 * 1024 * 1024;
			return new MultipartConfigElement("", maxSize, maxSize, 0);
		}
    }

    static class GZIPCompression {

        public static byte[] compress(final String str) throws IOException {
            if ((str == null) || (str.length() == 0)) {
                return null;
            }
            ByteArrayOutputStream obj = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(obj);
            gzip.write(str.getBytes("UTF-8"));
            gzip.close();
            return obj.toByteArray();
        }
    }

//class GZIPCompression {
//
//	public static byte[] compress(final String str) throws IOException {
//		if ((str == null) || (str.length() == 0)) {
//			return null;
//		}
//		ByteArrayOutputStream obj = new ByteArrayOutputStream();
//		GZIPOutputStream gzip = new GZIPOutputStream(obj);
//		gzip.write(str.getBytes("UTF-8"));
//		gzip.close();
//		return obj.toByteArray();
//	}
//>>>>>>> origin/1.4.x:spring-cloud-netflix-core/src/test/java/org/springframework/cloud/netflix/zuul/filters/route/SimpleHostRoutingFilterTests.java
}
