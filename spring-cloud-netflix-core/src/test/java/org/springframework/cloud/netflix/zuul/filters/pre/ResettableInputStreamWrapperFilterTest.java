package org.springframework.cloud.netflix.zuul.filters.pre;

import com.netflix.zuul.context.RequestContext;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.springframework.web.servlet.DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE;

/**
 * @author Andre Dörnbrack
 */
public class ResettableInputStreamWrapperFilterTest {

    private static byte[] TEST_CONTENT = {45, 45, 45, 45};

    ResettableInputStreamWrapperFilter filter = new ResettableInputStreamWrapperFilter();

    @Test
    public void testShouldFilter() throws Exception {
        givenRequestContextContainsJsonPostRequestWithBody(null);
        assertFalse(filter.shouldFilter());

        givenRequestContextContainsJsonPostRequestWithBody(new byte[]{});
        assertTrue(filter.shouldFilter());
        
        givenRequestContextContainsJsonPostRequestWithBody(TEST_CONTENT);
        assertTrue(filter.shouldFilter());

        givenRequestContextContainsMultipartPostRequest();
        assertTrue(filter.shouldFilter());

        givenRequestContextContainsUrlFormEncodedPostRequest();
        assertTrue(filter.shouldFilter());
    }

    @Test
    public void testMultipleReadsOnRequestInputStream() throws Exception {
        givenRequestContextContainsUrlFormEncodedPostRequest();
        filter.run();

        HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
        ServletInputStream inputStream = request.getInputStream();

        assertTrue(inputStream instanceof ResettableServletInputStreamWrapper);

        whenInputStreamIsConsumed(inputStream);
        assertEquals(-1, inputStream.read());

        inputStream.reset();
        assertNotEquals(-1, inputStream.read());

        whenInputStreamIsConsumed(inputStream);
        assertEquals(-1, inputStream.read());

        inputStream.reset();
        assertNotEquals(-1, inputStream.read());
    }

    private void whenInputStreamIsConsumed(ServletInputStream inputStream) throws IOException {
        while(inputStream.read() != -1) {
            inputStream.read();
        }
    }

    private void givenRequestContextContainsJsonPostRequestWithBody(byte[] body) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        request.setMethod(HttpMethod.POST.toString());
        request.setContent(body);

        RequestContext currentContext = RequestContext.getCurrentContext();
        currentContext.clear();
        currentContext.setRequest(request);
    }

    private void givenRequestContextContainsUrlFormEncodedPostRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        request.setMethod(HttpMethod.POST.toString());
        request.setContent(TEST_CONTENT);

        RequestContext currentContext = RequestContext.getCurrentContext();
        currentContext.clear();
        currentContext.setRequest(request);
    }
    
    private void givenRequestContextContainsMultipartPostRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType(MediaType.MULTIPART_FORM_DATA_VALUE);
        request.setMethod(HttpMethod.POST.toString());
        request.setAttribute(WEB_APPLICATION_CONTEXT_ATTRIBUTE, "true");

        Map<String, List<String>> firstPartHeaders = new HashMap<>();
        byte[] firstPartBody = "{ \"u\" : 1 }".getBytes();
        Part firstPart = new MockPart("a", "application/json", null, firstPartHeaders, firstPartBody);
        request.addPart(firstPart);

        Map<String, List<String>> secondPartHeaders = new HashMap<>();
        byte[] secondPartBody = "%PDF...1".getBytes();
        Part secondPart = new MockPart("b", "application/pdf", "document.pdf", secondPartHeaders, secondPartBody);
        request.addPart(secondPart);

        Map<String, List<String>> thirdPartHeaders = new HashMap<>();
        byte[] thirdPartBody = "%PDF...2".getBytes();
        Part thirdPart = new MockPart("c", "application/pdf", "attachment1.pdf", thirdPartHeaders, thirdPartBody);
        request.addPart(thirdPart);

        RequestContext currentContext = RequestContext.getCurrentContext();
        currentContext.clear();
        currentContext.setRequest(request);
    }
}