package org.springframework.cloud.netflix.zuul.filters.pre;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Part;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartHttpServletRequest;

import com.netflix.zuul.context.RequestContext;

/**
 * @author Michael Hartle
 */
public class FormBodyWrapperFilterTests {
	
	private FormBodyWrapperFilter filter;
	
	private MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();

	@Before
	public void init() {
		this.filter = new FormBodyWrapperFilter();
		RequestContext ctx = RequestContext.getCurrentContext();
		ctx.clear();
		ctx.setRequest(this.request);
	}
	
	@Test
	public void multiplePartNamesWithMultipleParts() throws IOException, ServletException {
		this.request.setRequestURI("/api/foo/1");
		this.request.setRemoteAddr("5.6.7.8");
		
		final Map<String, List<String>> firstPartHeaders = new HashMap<>();
		final byte[] firstPartBody = "{ \"u\" : 1 }".getBytes();
		final Part firstPart = new MockPart("a", "application/json", null, firstPartHeaders, firstPartBody);
		this.request.addPart(firstPart);

		final Map<String, List<String>> secondPartHeaders = new HashMap<>();
		final byte[] secondPartBody = "%PDF...1".getBytes();
		final Part secondPart = new MockPart("b", "application/pdf", "document.pdf", secondPartHeaders, secondPartBody);
		this.request.addPart(secondPart);
		
		final Map<String, List<String>> thirdPartHeaders = new HashMap<>();
		final byte[] thirdPartBody = "%PDF...2".getBytes();
		final Part thirdPart = new MockPart("c", "application/pdf", "attachment1.pdf", thirdPartHeaders, thirdPartBody);
		this.request.addPart(thirdPart);
		
		final Map<String, List<String>> fourthPartHeaders = new HashMap<>();
		final byte[] fourthPartBody = "%PDF...3".getBytes();
		final Part fourthPart = new MockPart("c", "application/pdf", "attachment2.pdf", fourthPartHeaders, fourthPartBody);
		this.request.addPart(fourthPart);

		final Map<String, List<String>> fifthPartHeaders = new HashMap<>();
		final byte[] fifthPartBody = "%PDF...4".getBytes();
		final Part fifthPart = new MockPart("c", "application/pdf", "attachment3.pdf", fifthPartHeaders, fifthPartBody);
		this.request.addPart(fifthPart);
		
		this.filter.run();
		
		final RequestContext ctx = RequestContext.getCurrentContext();
		assertEquals("/api/foo/1", ctx.getRequest().getRequestURI());
		assertEquals("5.6.7.8", ctx.getRequest().getRemoteAddr());
		assertEquals(5, ctx.getRequest().getParts().size());
		
		final Part[] parts = ctx.getRequest().getParts().toArray(new Part[0]);
		assertEquals("a", parts[0].getName());
		assertEquals(null, parts[0].getSubmittedFileName());
		assertEquals("application/json", parts[0].getContentType());
		assertArrayEquals(firstPartBody, IOUtils.toByteArray(parts[0].getInputStream()));
		
		assertEquals("b", parts[1].getName());
		assertEquals("document.pdf", parts[1].getSubmittedFileName());
		assertEquals("application/pdf", parts[1].getContentType());
		assertArrayEquals(secondPartBody, IOUtils.toByteArray(parts[1].getInputStream()));
		
		assertEquals("c", parts[2].getName());
		assertEquals("attachment1.pdf", parts[2].getSubmittedFileName());
		assertEquals("application/pdf", parts[2].getContentType());
		assertArrayEquals(thirdPartBody, IOUtils.toByteArray(parts[2].getInputStream()));
		
		assertEquals("c", parts[3].getName());
		assertEquals("attachment2.pdf", parts[3].getSubmittedFileName());
		assertEquals("application/pdf", parts[3].getContentType());
		assertArrayEquals(fourthPartBody, IOUtils.toByteArray(parts[3].getInputStream()));

		assertEquals("c", parts[4].getName());
		assertEquals("attachment3.pdf", parts[4].getSubmittedFileName());
		assertEquals("application/pdf", parts[4].getContentType());
		assertArrayEquals(fifthPartBody, IOUtils.toByteArray(parts[4].getInputStream()));
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
