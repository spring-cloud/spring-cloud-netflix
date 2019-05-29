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

package org.springframework.cloud.netflix.zuul.filters.route;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.netflix.zuul.test.NoSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Andreas Kluth
 * @author Spencer Gibb
 * @author Gang Li
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT,
		properties = { "server.servlet.context-path: /app" })
@DirtiesContext
public class SimpleHostRoutingFilterIntegrationTests {

	@LocalServerPort
	private int port;

	/*
	 * @Autowired private SimpleHostRoutingFilter filter;
	 *
	 * @Before public void setup() { CounterFactory.initialize(new EmptyCounterFactory());
	 * RequestContext.testSetCurrentContext(new RequestContext()); }
	 *
	 * @After public void clear() { CounterFactory.initialize(null);
	 *
	 * RequestContext.testSetCurrentContext(null);
	 * RequestContext.getCurrentContext().clear(); }
	 *
	 * @Test public void contentLengthNegativeTest() throws IOException {
	 * contentLengthTest(-1000L); }
	 *
	 * @Test public void contentLengthNegativeOneTest() throws IOException {
	 * contentLengthTest(-1L); }
	 *
	 * @Test public void contentLengthZeroTest() throws IOException {
	 * contentLengthTest(0L); }
	 *
	 * @Test public void contentLengthOneTest() throws IOException {
	 * contentLengthTest(1L); }
	 *
	 * @Test public void contentLength1KbTest() throws IOException {
	 * contentLengthTest(1000L); }
	 *
	 * @Test public void contentLength1MbTest() throws IOException {
	 * contentLengthTest(1000000L); }
	 *
	 * @Test public void contentLength1GbTest() throws IOException {
	 * contentLengthTest(1000000000L); }
	 *
	 * @Test public void contentLength2GbTest() throws IOException {
	 * contentLengthTest(2000000000L); }
	 *
	 * @Test public void contentLength3GbTest() throws IOException {
	 * contentLengthTest(3000000000L); }
	 *
	 * @Test public void contentLength4GbTest() throws IOException {
	 * contentLengthTest(4000000000L); }
	 *
	 * @Test public void contentLength5GbTest() throws IOException {
	 * contentLengthTest(5000000000L); }
	 *
	 * @Test public void contentLength6GbTest() throws IOException {
	 * contentLengthTest(6000000000L); }
	 *
	 * @Test public void contentLengthServlet30WithHeaderNegativeTest() throws IOException
	 * { contentLengthServlet30WithHeaderTest(-1000L); }
	 *
	 * @Test public void contentLengthServlet30WithHeaderNegativeOneTest() throws
	 * IOException { contentLengthServlet30WithHeaderTest(-1L); }
	 *
	 * @Test public void contentLengthServlet30WithHeaderZeroTest() throws IOException {
	 * contentLengthServlet30WithHeaderTest(0L); }
	 *
	 * @Test public void contentLengthServlet30WithHeaderOneTest() throws IOException {
	 * contentLengthServlet30WithHeaderTest(1L); }
	 *
	 * @Test public void contentLengthServlet30WithHeader1KbTest() throws IOException {
	 * contentLengthServlet30WithHeaderTest(1000L); }
	 *
	 * @Test public void contentLengthServlet30WithHeader1MbTest() throws IOException {
	 * contentLengthServlet30WithHeaderTest(1000000L); }
	 *
	 * @Test public void contentLengthServlet30WithHeader1GbTest() throws IOException {
	 * contentLengthServlet30WithHeaderTest(1000000000L); }
	 *
	 * @Test public void contentLengthServlet30WithHeader2GbTest() throws IOException {
	 * contentLengthServlet30WithHeaderTest(2000000000L); }
	 *
	 * @Test public void contentLengthServlet30WithHeader3GbTest() throws IOException {
	 * contentLengthServlet30WithHeaderTest(3000000000L); }
	 *
	 * @Test public void contentLengthServlet30WithHeader4GbTest() throws IOException {
	 * contentLengthServlet30WithHeaderTest(4000000000L); }
	 *
	 * @Test public void contentLengthServlet30WithHeader5GbTest() throws IOException {
	 * contentLengthServlet30WithHeaderTest(5000000000L); }
	 *
	 * @Test public void contentLengthServlet30WithHeader6GbTest() throws IOException {
	 * contentLengthServlet30WithHeaderTest(6000000000L); }
	 *
	 * @Test public void contentLengthServlet30WithInvalidLongHeaderTest() throws
	 * IOException { setupContext(); MockMultipartHttpServletRequest request =
	 * getMockedReqest(-1L); request.addHeader(HttpHeaders.CONTENT_LENGTH, "InvalidLong");
	 * contentLengthTest(-1L, getServlet30Filter(), request); }
	 *
	 * @Test public void contentLengthServlet30WithoutHeaderNegativeTest() throws
	 * IOException { contentLengthServlet30WithoutHeaderTest(-1000L); }
	 *
	 * @Test public void contentLengthServlet30WithoutHeaderNegativeOneTest() throws
	 * IOException { contentLengthServlet30WithoutHeaderTest(-1L); }
	 *
	 * @Test public void contentLengthServlet30WithoutHeaderZeroTest() throws IOException
	 * { contentLengthServlet30WithoutHeaderTest(0L); }
	 *
	 * @Test public void contentLengthServlet30WithoutHeaderOneTest() throws IOException {
	 * contentLengthServlet30WithoutHeaderTest(1L); }
	 *
	 * @Test public void contentLengthServlet30WithoutHeader1KbTest() throws IOException {
	 * contentLengthServlet30WithoutHeaderTest(1000L); }
	 *
	 * @Test public void contentLengthServlet30WithoutHeader1MbTest() throws IOException {
	 * contentLengthServlet30WithoutHeaderTest(1000000L); }
	 *
	 * @Test public void contentLengthServlet30WithoutHeader1GbTest() throws IOException {
	 * contentLengthServlet30WithoutHeaderTest(1000000000L); }
	 *
	 * @Test public void contentLengthServlet30WithoutHeader2GbTest() throws IOException {
	 * contentLengthServlet30WithoutHeaderTest(2000000000L); }
	 *
	 * @Test public void contentLengthServlet30WithoutHeader3GbTest() throws IOException {
	 * contentLengthServlet30WithoutHeaderTest(3000000000L); }
	 *
	 * @Test public void contentLengthServlet30WithoutHeader4GbTest() throws IOException {
	 * contentLengthServlet30WithoutHeaderTest(4000000000L); }
	 *
	 * @Test public void contentLengthServlet30WithoutHeader5GbTest() throws IOException {
	 * contentLengthServlet30WithoutHeaderTest(5000000000L); }
	 *
	 * @Test public void contentLengthServlet30WithoutHeader6GbTest() throws IOException {
	 * contentLengthServlet30WithoutHeaderTest(6000000000L); }
	 *
	 * public void contentLengthTest(Long contentLength) throws IOException {
	 * setupContext();
	 *
	 * contentLengthTest(contentLength, getFilter(), getMockedReqest(contentLength)); }
	 *
	 * public void contentLengthServlet30WithHeaderTest(Long contentLength) throws
	 * IOException { setupContext(); MockMultipartHttpServletRequest request =
	 * getMockedReqest(contentLength); request.addHeader(HttpHeaders.CONTENT_LENGTH,
	 * contentLength); contentLengthTest(contentLength, getServlet30Filter(), request); }
	 *
	 * public void contentLengthServlet30WithoutHeaderTest(Long contentLength) throws
	 * IOException { setupContext();
	 *
	 * //Although contentLength.intValue is not always equals to contentLength, that's the
	 * expected result when calling // request.getContentLength() from servlet 3.0
	 * implementation. contentLengthTest(Long.parseLong("" + contentLength.intValue()),
	 * getServlet30Filter(), getMockedReqest(contentLength)); }
	 *
	 * public MockMultipartHttpServletRequest getMockedReqest(final Long contentLength)
	 * throws IOException {
	 *
	 * MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest() {
	 *
	 * @Override public int getContentLength() { return contentLength.intValue(); }
	 *
	 * @Override public long getContentLengthLong() { return contentLength; } };
	 *
	 * return request; }
	 *
	 * public void contentLengthTest(Long expectedContentLength, SimpleHostRoutingFilter
	 * filter, MockMultipartHttpServletRequest request) throws IOException { byte[] data =
	 * "poprqwueproqiwuerpoqweiurpo".getBytes(); MockMultipartFile file = new
	 * MockMultipartFile("test.zip", "test.zip", "application/zip", data); String boundary
	 * = "q1w2e3r4t5y6u7i8o9"; request.setContentType("multipart/form-data; boundary=" +
	 * boundary); request.setContent( createFileContent(data, boundary, "application/zip",
	 * "test.zip")); request.addFile(file); request.setMethod("POST");
	 * request.setParameter("variant", "php"); request.setParameter("os", "mac");
	 * request.setParameter("version", "3.4"); request.setRequestURI("/app/echo");
	 *
	 * MockHttpServletResponse response = new MockHttpServletResponse();
	 * RequestContext.getCurrentContext().setRequest(request);
	 * RequestContext.getCurrentContext().setResponse(response); URL url = new
	 * URL("http://localhost:" + this.port);
	 * RequestContext.getCurrentContext().set("routeHost", url); filter.run();
	 *
	 * CloseableHttpResponse httpResponse = (CloseableHttpResponse)
	 * RequestContext.getCurrentContext() .get("zuulResponse");
	 * assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(200)); InputStream
	 * zuulResponse = httpResponse.getEntity().getContent(); assertThat(zuulResponse,
	 * not(instanceOf(EmptyInputStream.class))); String responseString =
	 * IOUtils.toString(new GZIPInputStream( zuulResponse));
	 * assertTrue(!responseString.isEmpty()); if (expectedContentLength < 0) {
	 * assertThat(responseString, containsString("\"" +
	 * HttpHeaders.TRANSFER_ENCODING.toLowerCase() + "\":\"chunked\""));
	 * assertThat(responseString,
	 * not(containsString(HttpHeaders.CONTENT_LENGTH.toLowerCase()))); } else {
	 * assertThat(responseString, containsString("\"" +
	 * HttpHeaders.CONTENT_LENGTH.toLowerCase() + "\":\"" + expectedContentLength +
	 * "\"")); } }
	 *
	 * public byte[] createFileContent(byte[] data, String boundary, String contentType,
	 * String fileName) { String start = "--" + boundary +
	 * "\r\n Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName +
	 * "\"\r\n" + "Content-type: " + contentType + "\r\n\r\n"; ;
	 *
	 * String end = "\r\n--" + boundary + "--"; // correction suggested @butfly return
	 * ArrayUtils.addAll(start.getBytes(), ArrayUtils.addAll(data, end.getBytes())); }
	 *
	 * @Test public void httpClientDoesNotDecompressEncodedData() throws Exception {
	 * setupContext(); InputStreamEntity inputStreamEntity = new InputStreamEntity(new
	 * ByteArrayInputStream(new byte[]{1})); HttpRequest httpRequest =
	 * getFilter().buildHttpRequest("GET", "/app/compressed/get/1", inputStreamEntity, new
	 * LinkedMultiValueMap<>(), new LinkedMultiValueMap<>(), new
	 * MockHttpServletRequest());
	 *
	 * CloseableHttpResponse response = getFilter().newClient().execute(new
	 * HttpHost("localhost", this.port), httpRequest); assertEquals(200,
	 * response.getStatusLine().getStatusCode()); byte[] responseBytes =
	 * copyToByteArray(response.getEntity().getContent());
	 * assertTrue(Arrays.equals(GZIPCompression.compress("Get 1"), responseBytes)); }
	 *
	 * @Test public void httpClientPreservesUnencodedData() throws Exception {
	 * setupContext(); InputStreamEntity inputStreamEntity = new InputStreamEntity(new
	 * ByteArrayInputStream(new byte[]{1})); HttpRequest httpRequest =
	 * getFilter().buildHttpRequest("GET", "/app/get/1", inputStreamEntity, new
	 * LinkedMultiValueMap<>(), new LinkedMultiValueMap<>(), new
	 * MockHttpServletRequest());
	 *
	 * CloseableHttpResponse response = getFilter().newClient().execute(new
	 * HttpHost("localhost", this.port), httpRequest); assertEquals(200,
	 * response.getStatusLine().getStatusCode()); String responseString =
	 * copyToString(response.getEntity().getContent(), Charset.forName("UTF-8"));
	 * assertTrue("Get 1".equals(responseString)); }
	 *
	 * @Test public void redirectTest() throws IOException { setupContext();
	 * InputStreamEntity inputStreamEntity = new InputStreamEntity(new
	 * ByteArrayInputStream(new byte[]{})); HttpRequest httpRequest =
	 * getFilter().buildHttpRequest("GET", "/app/redirect", inputStreamEntity, new
	 * LinkedMultiValueMap<>(), new LinkedMultiValueMap<>(), new
	 * MockHttpServletRequest());
	 *
	 * CloseableHttpResponse response = getFilter().newClient().execute(new
	 * HttpHost("localhost", this.port), httpRequest); assertEquals(302,
	 * response.getStatusLine().getStatusCode()); String responseString =
	 * copyToString(response.getEntity().getContent(), Charset.forName("UTF-8"));
	 * assertTrue(response.getLastHeader("Location").getValue().contains("/app/get/5")); }
	 *
	 * @Test(expected = ZuulRuntimeException.class) public void run() throws Exception {
	 * setupContext(); MockHttpServletRequest request = new MockHttpServletRequest("POST",
	 * "/"); request.setContent("{1}".getBytes()); request.addHeader("singleName",
	 * "singleValue"); request.addHeader("multiName", "multiValue1");
	 * request.addHeader("multiName", "multiValue2");
	 * RequestContext.getCurrentContext().setRequest(request); URL url = new
	 * URL("http://localhost:8080"); RequestContext.getCurrentContext().set("routeHost",
	 * url); getFilter().run(); }
	 *
	 * private void setupContext() { }
	 *
	 * private SimpleHostRoutingFilter getFilter() { return this.filter; }
	 *
	 * private SimpleHostRoutingFilter getServlet30Filter() { SimpleHostRoutingFilter
	 * filter = getFilter(); filter.setUseServlet31(false); return filter; }
	 */
	/*
	 * @Configuration
	 *
	 * @EnableConfigurationProperties protected static class TestConfiguration {
	 *
	 * @Bean ZuulProperties zuulProperties() { return new ZuulProperties(); }
	 *
	 * @Bean ApacheHttpClientFactory clientFactory() {return new
	 * DefaultApacheHttpClientFactory(HttpClientBuilder.create()); }
	 *
	 * @Bean ApacheHttpClientConnectionManagerFactory connectionManagerFactory() { return
	 * new DefaultApacheHttpClientConnectionManagerFactory(); }
	 *
	 * @Bean SimpleHostRoutingFilter simpleHostRoutingFilter(ZuulProperties
	 * zuulProperties, ApacheHttpClientConnectionManagerFactory connectionManagerFactory,
	 * ApacheHttpClientFactory clientFactory) { return new SimpleHostRoutingFilter(new
	 * ProxyRequestHelper(), zuulProperties, connectionManagerFactory, clientFactory); } }
	 */

	@Test
	public void test() {

	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@RestController
	@Import(NoSecurityConfiguration.class)
	static class SampleApplication {

		@RequestMapping(value = "/compressed/get/{id}", method = RequestMethod.GET)
		public byte[] getCompressed(@PathVariable String id, HttpServletResponse response)
				throws IOException {
			response.setHeader("content-encoding", "gzip");
			return GZIPCompression.compress("Get " + id);
		}

		@RequestMapping(value = "/get/{id}", method = RequestMethod.GET)
		public String getString(@PathVariable String id, HttpServletResponse response)
				throws IOException {
			return "Get " + id;
		}

		@RequestMapping(value = "/redirect", method = RequestMethod.GET)
		public String redirect(HttpServletResponse response) throws IOException {
			response.sendRedirect("/app/get/5");
			return null;
		}

		@RequestMapping("/echo")
		public Map<String, Object> echoRequestAttributes(
				@RequestHeader HttpHeaders httpHeaders, HttpServletRequest request)
				throws IOException {
			Map<String, Object> result = new HashMap<>();
			result.put("headers", httpHeaders.toSingleValueMap());

			return result;
		}

		@Bean
		MultipartConfigElement multipartConfigElement() {
			long maxSize = 10L * 1024 * 1024 * 1024;
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

}
