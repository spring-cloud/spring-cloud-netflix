package org.springframework.cloud.netflix.zuul.filters.pre;

import com.netflix.zuul.context.RequestContext;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Michael Hartle
 * @author Andre Dörnbrack
 */
public class FormBodyWrapperFilterTests {

	private FormBodyWrapperFilter filter = new FormBodyWrapperFilter();

	private static final byte[] TEST_BODY = {45, 45, 45, 45, 45};
	private static final byte[] FIRST_PART_BODY = "{ \"u\" : 1 }".getBytes();
	private static final byte[] SECOND_PART_BODY = "%PDF...1".getBytes();

	@Test
	public void shouldFilterOnPositivContentLength() throws Exception {
		givenRequestContextContainsMediatypeJsonPostRequestWithBody(null);
		assertFalse(filter.shouldFilter());

		givenRequestContextContainsMediatypeJsonPostRequestWithBody(new byte[]{});
		assertTrue(filter.shouldFilter());

		givenRequestContextContainsMediatypeJsonPostRequestWithBody(TEST_BODY);
		assertTrue(filter.shouldFilter());
	}

	@Test
	public void shouldFilterOnMultipartContentType() throws Exception {
		givenRequestContextContainsGetRequest();
		assertFalse(filter.shouldFilter());

		givenRequestContextContainsMultipartPostRequest();
		assertTrue(filter.shouldFilter());
	}

	@Test
	public void shouldFilterOnUrlFormEncodedContentType() throws Exception {
		givenRequestContextContainsGetRequest();
		assertFalse(filter.shouldFilter());

		givenRequestContextContainsUrlFormEncodedPostRequest();
		assertTrue(filter.shouldFilter());
	}

	@Test
	public void multipleReadsOnInputStream() throws Exception {
		givenRequestContextContainsMultipartPostRequest();

		filter.run();

		HttpServletRequest requestInContext = RequestContext.getCurrentContext().getRequest();
		ServletInputStream inputStream = requestInContext.getInputStream();

		assertTrue(inputStream instanceof ResettableServletInputStreamWrapper);

		whenInputStreamIsConsumed(inputStream);
		assertEquals(inputStream.read(), -1);

		inputStream.reset();
		assertNotEquals(inputStream.read(), -1);

		whenInputStreamIsConsumed(inputStream);
		assertEquals(inputStream.read(), -1);

		inputStream.reset();
		assertNotEquals(inputStream.read(), -1);
	}

	@Test
	public void multiplePartNamesWithMultipleParts() throws IOException, ServletException {
		givenRequestContextContainsMultipartPostRequest();

		this.filter.run();

		RequestContext ctx = RequestContext.getCurrentContext();
		assertEquals("/api/foo/1", ctx.getRequest().getRequestURI());
		assertEquals("5.6.7.8", ctx.getRequest().getRemoteAddr());
		assertEquals(2, ctx.getRequest().getParts().size());

		Part[] parts = ctx.getRequest().getParts().toArray(new Part[0]);
		assertEquals("a", parts[0].getName());
		assertEquals(null, parts[0].getSubmittedFileName());
		assertEquals("application/json", parts[0].getContentType());
		assertArrayEquals(FIRST_PART_BODY, IOUtils.toByteArray(parts[0].getInputStream()));

		assertEquals("b", parts[1].getName());
		assertEquals("document.pdf", parts[1].getSubmittedFileName());
		assertEquals("application/pdf", parts[1].getContentType());
		assertArrayEquals(SECOND_PART_BODY, IOUtils.toByteArray(parts[1].getInputStream()));
	}

	private void givenRequestContextContainsGetRequest() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod(HttpMethod.GET.toString());

		setRequestOnContext(request);
	}

	private void givenRequestContextContainsUrlFormEncodedPostRequest() {
		MockHttpServletRequest request = new MockHttpServletRequest();

		request.setContentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
		request.setMethod(HttpMethod.POST.toString());
		request.setContent(TEST_BODY);

		setRequestOnContext(request);
	}

	private void givenRequestContextContainsMediatypeJsonPostRequestWithBody(byte[] body) {
		MockHttpServletRequest request = new MockHttpServletRequest();

		request.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, "true");
		request.setMethod(HttpMethod.POST.toString());
		request.setContent(body);

		setRequestOnContext(request);
	}

	private void givenRequestContextContainsMultipartPostRequest() {
		MockHttpServletRequest request = new MockHttpServletRequest();

		request.setRequestURI("/api/foo/1");
		request.setRemoteAddr("5.6.7.8");
		request.setContentType(MediaType.MULTIPART_FORM_DATA_VALUE);
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, "true");
		request.setMethod(HttpMethod.POST.toString());

		Map<String, List<String>> firstPartHeaders = new HashMap<>();
		Part firstPart = new MockPart("a", "application/json", null, firstPartHeaders, FIRST_PART_BODY);
		request.addPart(firstPart);

		Map<String, List<String>> secondPartHeaders = new HashMap<>();
		Part secondPart = new MockPart("b", "application/pdf", "document.pdf", secondPartHeaders, SECOND_PART_BODY);
		request.addPart(secondPart);

		setRequestOnContext(request);
	}

	private void setRequestOnContext(MockHttpServletRequest request) {
		RequestContext ctx = RequestContext.getCurrentContext();
		ctx.clear();
		ctx.setRequest(request);
	}

	private void whenInputStreamIsConsumed(ServletInputStream inputStream) throws IOException {
		while(inputStream.read() != -1) {
			inputStream.read();
		}
	}

	private class MockPart implements Part {
		private final String name;
		private final String contentType;
		private final String submittedFileName;
		private final Map<String, List<String>> headers;
		private final byte[] body;

		public MockPart(final String name, final String contentType, final String submittedFileName, final Map<String, List<String>> headers, final byte[] body) {
			this.name = name;
			this.contentType = contentType;
			this.submittedFileName = submittedFileName;
			this.headers = headers;
			this.body = body;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return new ByteArrayInputStream(this.body);
		}

		@Override
		public String getContentType() {
			return this.contentType;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public String getSubmittedFileName() {
			return this.submittedFileName;
		}

		@Override
		public long getSize() {
			return this.body != null ? this.body.length : 0;
		}

		@Override
		public void write(String fileName) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void delete() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getHeader(String name) {
			if (this.headers == null) {
				return null;
			}

			final List<String> values = this.headers.get(name);

			if (values == null || values.size() == 0) {
				return null;
			}

			return values.get(0);
		}

		@Override
		public Collection<String> getHeaders(String name) {
			if (this.headers == null) {
				return null;
			}

			return this.headers.get(name);
		}

		@Override
		public Collection<String> getHeaderNames() {
			if (this.headers == null) {
				return null;
			}

			return this.headers.keySet();
		}
	}
}
