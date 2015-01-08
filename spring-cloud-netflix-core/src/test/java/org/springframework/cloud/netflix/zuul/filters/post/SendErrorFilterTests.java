package org.springframework.cloud.netflix.zuul.filters.post;

import com.netflix.zuul.context.RequestContext;
import org.junit.After;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Spencer Gibb
 */
public class SendErrorFilterTests {

	@After
	public void reset() {
		RequestContext.testSetCurrentContext(null);
	}

	@Test
	public void runsNormally() {
		SendErrorFilter filter = createSendErrorFilter(new MockHttpServletRequest());

		assertTrue("shouldFilter returned false", filter.shouldFilter());
		filter.run();
	}

	private SendErrorFilter createSendErrorFilter(HttpServletRequest request) {
		RequestContext context = new RequestContext();
		context.setRequest(request);
		context.setResponse(new MockHttpServletResponse());
		context.set("error.status_code", HttpStatus.NOT_FOUND.value());
		RequestContext.testSetCurrentContext(context);
		SendErrorFilter filter = new SendErrorFilter();
		filter.setErrorPath("/error");
		return filter;
	}

	@Test
	public void noRequestDispatcher() {
		SendErrorFilter filter = createSendErrorFilter(mock(HttpServletRequest.class));

		assertTrue("shouldFilter returned false", filter.shouldFilter());
		filter.run();
	}

	@Test
	public void doesNotRunTwice() {
		SendErrorFilter filter = createSendErrorFilter(new MockHttpServletRequest());

		assertTrue("shouldFilter returned false", filter.shouldFilter());
		filter.run();
		assertFalse("shouldFilter returned true", filter.shouldFilter());
	}
}
