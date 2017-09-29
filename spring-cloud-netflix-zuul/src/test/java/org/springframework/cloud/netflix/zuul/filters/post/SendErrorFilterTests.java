/*
 * Copyright 2013-2015 the original author or authors.
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
 */

package org.springframework.cloud.netflix.zuul.filters.post;

import javax.servlet.http.HttpServletRequest;

import com.netflix.zuul.exception.ZuulException;
import com.netflix.zuul.monitoring.MonitoringHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.netflix.zuul.context.RequestContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Spencer Gibb
 */
public class SendErrorFilterTests {

	@Before
	public void setTestRequestcontext() {
		MonitoringHelper.initMocks();
		RequestContext context = new RequestContext();
		RequestContext.testSetCurrentContext(context);
	}

	@After
	public void reset() {
		RequestContext.getCurrentContext().clear();
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
		context.setThrowable(new ZuulException(new RuntimeException(), HttpStatus.NOT_FOUND.value(), null));
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

	@Test
	public void setResponseCode() {
		SendErrorFilter filter = createSendErrorFilter(new MockHttpServletRequest());
		filter.run();

		RequestContext ctx = RequestContext.getCurrentContext();
		int resCode = ctx.getResponse().getStatus();
		int ctxCode = ctx.getResponseStatusCode();

		assertEquals("invalid response code: " + resCode, HttpStatus.NOT_FOUND.value(), resCode);
		assertEquals("invalid response code in RequestContext: " + ctxCode, resCode, ctxCode);
	}
}
