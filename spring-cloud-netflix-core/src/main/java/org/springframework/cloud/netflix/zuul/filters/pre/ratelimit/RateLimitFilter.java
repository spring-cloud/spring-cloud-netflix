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


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;


/**
 * @author Vinicius Carvalho
 */
public class RateLimitFilter extends ZuulFilter {

	private final RateLimiter limiter;

	private RateLimitProperties properties;

	public RateLimitFilter(RateLimiter limiter, RateLimitProperties properties) {
		this.limiter = limiter;
		this.properties = properties;
	}

	@Override
	public String filterType() {
		return "pre";
	}

	@Override
	public int filterOrder() {
		return -1;
	}

	@Override
	public boolean shouldFilter() {
		return true;
	}

	public Object run(){
		RequestContext ctx = RequestContext.getCurrentContext();
		HttpServletResponse response = ctx.getResponse();
		HttpServletRequest request = ctx.getRequest();
		Policy policy = findRequestPolicy(request);
		String key = findKey(request);
		Rate rate = limiter.consume(policy, key);
		response.setHeader(Headers.LIMIT, rate.getLimit().toString());
		response.setHeader(Headers.REMAINING, String.valueOf(Math.max(rate.getRemaining(), 0)));
		response.setHeader(Headers.RESET, rate.getReset().toString());
		if (rate.getRemaining() <= 0) {
			ctx.setResponseStatusCode(429);
			ctx.put("rateLimitExceeded","true");
			throw new RuntimeException("");
		}
		return null;
	}

	private Policy findRequestPolicy(HttpServletRequest request) {
		Policy policy = (request.getUserPrincipal() == null) ? properties.getPolicies().get(Policy.PolicyType.ANONYMOUS) : properties.getPolicies().get(Policy.PolicyType.AUTHENTICATED);
		return policy;
	}

	private String findKey(HttpServletRequest request) {
		String key = (request.getUserPrincipal() == null) ? request.getRemoteAddr() : request.getUserPrincipal().getName();
		return key;
	}

	public static interface Headers {
		String LIMIT = "X-RateLimit-Limit";

		String REMAINING = "X-RateLimit-Remaining";

		String RESET = "X-RateLimit-Reset";
	}
}
