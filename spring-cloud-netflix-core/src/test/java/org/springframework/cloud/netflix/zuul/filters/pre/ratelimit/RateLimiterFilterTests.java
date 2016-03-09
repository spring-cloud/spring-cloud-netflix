/*
 *  Copyright 2013-2015 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.springframework.cloud.netflix.zuul.filters.pre.ratelimit;

import com.netflix.util.Pair;
import com.netflix.zuul.context.RequestContext;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.security.Principal;
import java.util.List;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * @author Vinicius Carvalho
 */
public class RateLimiterFilterTests {

	private MockHttpServletRequest request = new MockHttpServletRequest();
	private MockHttpServletResponse response = new MockHttpServletResponse();
	private RateLimitProperties properties;
	@Before
	public void setup(){
		RequestContext ctx = RequestContext.getCurrentContext();
		ctx.clear();
		ctx.setRequest(this.request);
		ctx.setResponse(response);
		properties = new RateLimitProperties();
		properties.init();
	}

	@Test
	public void anonymousHeadersOk() throws Exception {
		RateLimiter limiter = new InMemoryRateLimiter();
		RateLimitFilter filter = new RateLimitFilter(limiter,properties);
		filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();

		Policy policy = properties.getPolicies().get(Policy.PolicyType.ANONYMOUS);
		assertNull(ctx.get("error.status_code"));
		verifyHeaders(ctx);
		assertEquals(policy.getLimit().toString(), ctx.getResponse().getHeader(RateLimitFilter.Headers.LIMIT));
		assertEquals(policy.getLimit()-1, Long.parseLong(ctx.getResponse().getHeader(RateLimitFilter.Headers.REMAINING)));
		assertTrue(Long.valueOf(ctx.getResponse().getHeader(RateLimitFilter.Headers.RESET)) > System.currentTimeMillis());
	}

	@Test
	public void anonymousTooManyRequests() throws Exception{
		RateLimiter limiter = mock(RateLimiter.class);

		Rate sample = new Rate(2L,0L,10L);

		when(limiter.consume(any(Policy.class),anyString())).thenReturn(sample);
		RateLimitFilter filter = new RateLimitFilter(limiter,properties);
		filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();

		assertEquals(sample.getLimit().toString(), ctx.getResponse().getHeader(RateLimitFilter.Headers.LIMIT));
		assertEquals(sample.getRemaining().toString(), ctx.getResponse().getHeader(RateLimitFilter.Headers.REMAINING));
		assertEquals(sample.getReset().toString(), ctx.getResponse().getHeader(RateLimitFilter.Headers.RESET));
		assertEquals(429,ctx.get("error.status_code"));
	}

	@Test
	public void authenticatedHeadersOk() throws Exception{
		Policy anonymoousPolicy = properties.getPolicies().get(Policy.PolicyType.ANONYMOUS);
		Policy authPolicy = new Policy(60L,600L);
		properties.getPolicies().put(Policy.PolicyType.AUTHENTICATED.toString(),authPolicy);

		RateLimiter limiter = new InMemoryRateLimiter();
		RateLimitFilter filter = new RateLimitFilter(limiter,properties);

		filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();
		verifyHeaders(ctx);
		String anonymousRemaining = ctx.getResponse().getHeader(RateLimitFilter.Headers.REMAINING);
		this.request.setUserPrincipal(new Principal() {
			@Override
			public String getName() {
				return "lucy";
			}
		});
		filter.run();
		String authRemaining = ctx.getResponse().getHeader(RateLimitFilter.Headers.REMAINING);
		assertTrue(Long.parseLong(authRemaining) > Long.parseLong(anonymousRemaining));

	}

	private Object getHeader(List<Pair<String, String>> headers, String key) {
		String value = null;
		for (Pair<String, String> pair : headers) {
			if (pair.first().toLowerCase().equals(key.toLowerCase())) {
				value = pair.second();
				break;
			}
		}
		return value;
	}

	private void verifyHeaders(RequestContext ctx){
		assertNotNull(ctx.getResponse().getHeader(RateLimitFilter.Headers.LIMIT));
		assertNotNull(ctx.getResponse().getHeader(RateLimitFilter.Headers.REMAINING));
		assertNotNull(ctx.getResponse().getHeader(RateLimitFilter.Headers.RESET));
	}

}
