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

import com.netflix.zuul.context.RequestContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Spencer Gibb
 */
public class SendErrorFilterTests {

	@Before
	public void setTestRequestcontext() {
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

	private SendErrorFilter createSendErrorFilter(RequestContext context) {
		RequestContext.testSetCurrentContext(context);
		SendErrorFilter filter = new SendErrorFilter();
		filter.setErrorPath("/error");
		return filter;
	}

	private SendErrorFilter createSendErrorFilter(HttpServletRequest request) {
		return createSendErrorFilter(requestContextWithErrorStatusCode(request));
	}

	private RequestContext requestContextWithErrorStatusCode(HttpServletRequest request) {
		RequestContext context = new RequestContext();
		context.setRequest(request);
		context.setResponse(new MockHttpServletResponse());
		context.set("error.status_code", HttpStatus.NOT_FOUND.value());
		return context;
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
	public void runsNormallyForResponseStatusWithError() {
		SendErrorFilter filter = createSendErrorFilter(requestContextWithErrorResponseStatus());
		assertTrue("shouldFilter returned false", filter.shouldFilter());
		filter.run();
	}

	private RequestContext requestContextWithErrorResponseStatus() {
		RequestContext context = new RequestContext();
		context.setRequest(new MockHttpServletRequest());
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setStatus(HttpStatus.NOT_FOUND.value());
		context.setResponse(response);
		return context;
	}
}
