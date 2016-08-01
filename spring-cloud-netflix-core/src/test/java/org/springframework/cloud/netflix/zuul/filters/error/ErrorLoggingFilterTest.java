package org.springframework.cloud.netflix.zuul.filters.error;

import com.netflix.zuul.context.RequestContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.OutputCapture;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ErrorLoggingFilterTest {

    @Rule
    public OutputCapture capture = new OutputCapture();

    private RequestContext context;

    @Before
    public void setTestRequestcontext() {
        context = new RequestContext();
        RequestContext.testSetCurrentContext(context);
    }

    @After
    public void reset() {
        RequestContext.getCurrentContext().clear();
    }

    @Test
    public void shouldLogExceptionIfPresentInContext() throws Exception {
        context.setThrowable(new IllegalStateException("Thrown by test"));

        ErrorLoggingFilter filter = new ErrorLoggingFilter();

        assertTrue("ShouldFilter must be true", filter.shouldFilter());

        filter.run();

        assertThat(capture.toString(), containsString("java.lang.IllegalStateException: Thrown by test"));

    }

    @Test
    public void shouldNotRunIfNoExceptionInContext() throws Exception {
        ErrorLoggingFilter filter = new ErrorLoggingFilter();

        assertFalse("ShouldFilter must be false", filter.shouldFilter());
    }
}