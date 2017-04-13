
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
import org.springframework.cloud.netflix.zuul.filters.ExposedPartialProperty;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.context.annotation.Bean;
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
@SpringBootTest(classes = {ExposedPartialAccessFilterTest.Config.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ExposedPartialAccessFilterTest {

    @Autowired
    private ExposedPartialProperty exposedPartialProperty;

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

    @Test
    public void filterShouldPermitUserWithCorrectHeaders() {
        MockHttpServletRequest request = createRequest("/machine", true);
        ExposedPartialAccessFilter filter = createPartialAccessFilter(request);
        assertTrue("shouldFilter returned false", filter.shouldFilter());
        filter.run();
    }

    @Test(expected = AccessDeniedException.class)
    public void filterShouldReturnUnauthorizedWithoutAuthHeaders() {
        MockHttpServletRequest request = createRequest("/machine", false);
        ExposedPartialAccessFilter filter = createPartialAccessFilter(request);
        assertTrue("shouldFilter returned false", filter.shouldFilter());
        filter.run();
    }

    @Test(expected = AccessDeniedException.class)
    public void filterShouldNotPermitUserWithWrongHeaderValue() {
        MockHttpServletRequest request = createRequest("/machine", false);
        request.addHeader("Authorization", "Beare eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ");
        ExposedPartialAccessFilter filter = createPartialAccessFilter(request);
        assertTrue("shouldFilter returned false", filter.shouldFilter());
        filter.run();
    }

    @Test(expected = AccessDeniedException.class)
    public void filterShouldNotPermitUserWithoutAuthHeaderValueAndWithoutPublicPath() {
        MockHttpServletRequest request = createRequest("/machine-without-config", false);
        ExposedPartialAccessFilter filter = createPartialAccessFilter(request);
        assertTrue("shouldFilter returned false", filter.shouldFilter());
        filter.run();
    }

    @Test
    public void configuredPathShouldPermitUserWithoutAuthHeaders() {
        MockHttpServletRequest request = createRequest("/machine/api", false);
        ExposedPartialAccessFilter filter = createPartialAccessFilter(request);
        assertTrue("shouldFilter returned false", filter.shouldFilter());
        filter.run();
    }

    @Test
    public void filterShouldNotTriggerOnEmptyPath() {
        MockHttpServletRequest request = createRequest("", false);
        ExposedPartialAccessFilter filter = createPartialAccessFilter(request);
        assertFalse("shouldFilter returned true", filter.shouldFilter());
    }

    @Test
    public void filterShouldNotTriggerOnInvalidPath() {
        MockHttpServletRequest request = createRequest("invalidPath", false);
        ExposedPartialAccessFilter filter = createPartialAccessFilter(request);
        assertFalse("shouldFilter returned true", filter.shouldFilter());
    }

    @Test
    public void filterShouldNotTriggerOnWrongPath() {
        MockHttpServletRequest request = createRequest("/wrongPath", false);
        ExposedPartialAccessFilter filter = createPartialAccessFilter(request);
        assertFalse("shouldFilter returned true", filter.shouldFilter());
    }

    @Test
    public void filterShouldNotTriggerOnFullAccessLevel() {
        MockHttpServletRequest request = createRequest("/rental/api", false);
        ExposedPartialAccessFilter filter = createPartialAccessFilter(request);
        assertFalse("shouldFilter returned true", filter.shouldFilter());
    }

    @Test
    public void filterShouldNotTriggerOnPublicAccessLevel() {
        MockHttpServletRequest request = createRequest("/my-asset-planner", false);
        ExposedPartialAccessFilter filter = createPartialAccessFilter(request);
        assertFalse("shouldFilter returned true", filter.shouldFilter());
    }

    @Test
    public void filterShouldNotTriggerOnPartialPrivateLevel() {
        MockHttpServletRequest request = createRequest("/application", false);
        ExposedPartialAccessFilter filter = createPartialAccessFilter(request);
        assertFalse("shouldFilter returned true", filter.shouldFilter());
    }

    private MockHttpServletRequest createRequest(String servletPath, boolean authorized) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath(servletPath);
        if (authorized) {
            request.addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ");
        }
        return request;
    }

    private ExposedPartialAccessFilter createPartialAccessFilter(HttpServletRequest request) {
        RequestContext context = new RequestContext();
        context.setRequest(request);
        context.setResponse(new MockHttpServletResponse());
        RequestContext.testSetCurrentContext(context);
        return new ExposedPartialAccessFilter(zuulProperties, exposedPartialProperty);
    }


    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableZuulProxy
    protected static class Config {

        @Bean
        public ZuulProperties zuulProperties() {
            return new ZuulProperties();
        }

        @Bean
        public ExposedPartialProperty exposedPartialPropertyConfiguration() {
            return new ExposedPartialProperty();
        }

        @Bean
        public ExposedPartialAccessFilter testPreFilter() {
            return new ExposedPartialAccessFilter(zuulProperties(), exposedPartialPropertyConfiguration());
        }
    }
}

