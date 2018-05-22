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

package org.springframework.cloud.netflix.zuul.filters.route;

import javax.servlet.http.HttpServletRequest;

import com.netflix.zuul.context.RequestContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.FORWARD_TO_KEY;

/**
 * @author Dave Syer
 */
public class SendForwardFilterTests {

	@After
	public void reset() {
		RequestContext.getCurrentContext().clear();
	}

	@Before
	public void setTestRequestcontext() {
		RequestContext context = new RequestContext();
		RequestContext.testSetCurrentContext(context);
	}

	@Test
	public void runsNormally() {
		SendForwardFilter filter = createSendForwardFilter(new MockHttpServletRequest());
		assertTrue("shouldFilter returned false", filter.shouldFilter());
		filter.run();
	}

	private SendForwardFilter createSendForwardFilter(HttpServletRequest request) {
		RequestContext context = new RequestContext();
		context.setRequest(request);
		context.setResponse(new MockHttpServletResponse());
		context.set(FORWARD_TO_KEY, "/foo");
		RequestContext.testSetCurrentContext(context);
		SendForwardFilter filter = new SendForwardFilter();
		return filter;
	}

	@Test
	public void doesNotRunTwice() {
		SendForwardFilter filter = createSendForwardFilter(new MockHttpServletRequest());
		assertTrue("shouldFilter returned false", filter.shouldFilter());
		filter.run();
		assertFalse("shouldFilter returned true", filter.shouldFilter());
	}
}
