package org.springframework.cloud.netflix.zuul.filters.pre;

import com.netflix.zuul.context.RequestContext;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartHttpServletRequest;

import javax.servlet.ServletException;
import javax.servlet.http.Part;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

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
}
