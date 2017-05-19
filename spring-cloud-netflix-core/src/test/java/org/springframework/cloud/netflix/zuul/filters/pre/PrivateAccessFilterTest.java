
package org.springframework.cloud.netflix.zuul.filters.pre;

import com.netflix.zuul.context.RequestContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.junit4.SpringRunner;

import javax.servlet.http.HttpServletRequest;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

/**
 * @author Kevin Van Houtte
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {PrivateAccessFilterTest.Config.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PrivateAccessFilterTest {

    @Autowired
    private ZuulProperties zuulProperties;

    @After
    public void reset() {
        RequestContext.testSetCurrentContext(null);
    }

    @Before
    public void setTestRequestcontext() {
        RequestContext context = new RequestContext();
        RequestContext.testSetCurrentContext(context);
    }

    @Test(expected = AccessDeniedException.class)
    public void filterShouldReturnUnauthorizedWithoutAuthHeaders() {
        MockHttpServletRequest request = createRequest("/reservation", false);
        PrivateAccessFilter filter = createFullAccessFilter(request);
        assertTrue("shouldFilter returned false", filter.shouldFilter());
        filter.run();
    }

    @Test
    public void filterShouldPermitUserWithCorrectHeaders() {
        MockHttpServletRequest request = createRequest("/reservation", true);
        PrivateAccessFilter filter = createFullAccessFilter(request);
        assertTrue("shouldFilter returned false", filter.shouldFilter());
        filter.run();
    }

    @Test(expected = AccessDeniedException.class)
    public void filterShouldNotPermitUserWithWrongHeaderValue() {
        MockHttpServletRequest request = createRequest("/reservation", false);
        request.addHeader("Authorization", "Beare eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ");
        PrivateAccessFilter filter = createFullAccessFilter(request);
        assertTrue("shouldFilter returned false", filter.shouldFilter());
        filter.run();
    }

    @Test
    public void filterShouldNotTriggerOnEmptyPath() {
        MockHttpServletRequest request = createRequest("", false);
        PrivateAccessFilter filter = createFullAccessFilter(request);
        assertFalse("shouldFilter returned true", filter.shouldFilter());
    }

    @Test
    public void filterShouldNotTriggerOnInvalidPath() {
        MockHttpServletRequest request = createRequest("invalidPath", false);
        PrivateAccessFilter filter = createFullAccessFilter(request);
        assertFalse("shouldFilter returned true", filter.shouldFilter());
    }

    @Test
    public void filterShouldNotTriggerOnWrongPath() {
        MockHttpServletRequest request = createRequest("/wrongPath", false);
        PrivateAccessFilter filter = createFullAccessFilter(request);
        assertFalse("shouldFilter returned true", filter.shouldFilter());
    }

    @Test
    public void filterShouldNotTriggerOnPartialAccessLevel() {
        MockHttpServletRequest request = createRequest("/machine", false);
        PrivateAccessFilter filter = createFullAccessFilter(request);
        assertFalse("shouldFilter returned true", filter.shouldFilter());
    }

    @Test
    public void filterShouldNotTriggerOnPublicAccessLevel() {
        MockHttpServletRequest request = createRequest("/stores", false);
        PrivateAccessFilter filter = createFullAccessFilter(request);
        assertFalse("shouldFilter returned true", filter.shouldFilter());
    }

    private PrivateAccessFilter createFullAccessFilter(HttpServletRequest request) {
        RequestContext context = new RequestContext();
        context.setRequest(request);
        context.setResponse(new MockHttpServletResponse());
        RequestContext.testSetCurrentContext(context);
        return new PrivateAccessFilter(zuulProperties);
    }

    private MockHttpServletRequest createRequest(String servletPath, boolean authorized) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath(servletPath);
        if (authorized) {
            request.addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ");
        }
        return request;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableZuulProxy
    protected static class Config {


    }
}


