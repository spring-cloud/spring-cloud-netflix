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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author Vinicius Carvalho
 */
@Component
@ConditionalOnProperty("zuul.ratelimit.enabled")
public class RateLimitFilter extends ZuulFilter {

	private final RateLimiter limiter;
	private RateLimitProperties properties;

	@Autowired
	public RateLimitFilter(RateLimiter limiter, RateLimitProperties properties){
		this.limiter = limiter;
		this.properties = properties;
	}

	@Override
	public String filterType() {
		return "post";
	}

	@Override
	public int filterOrder() {
		return -1;
	}

	@Override
	public boolean shouldFilter() {
		return true;
	}

	@Override
	public Object run() {
		RequestContext ctx = RequestContext.getCurrentContext();
		HttpServletResponse response  = ctx.getResponse();
		HttpServletRequest request = ctx.getRequest();
		Policy policy = findRequestPolicy(request);
		String key = request.getRemoteAddr();
		Rate rate = limiter.consume(policy,key);
		response.setHeader("X-RateLimit-Limit",rate.getLimit().toString());
		response.setHeader("X-RateLimit-Remaining",Math.max(rate.getRemaining(),0)+"");
		response.setHeader("X-RateLimit-Reset",rate.getReset().toString());
		if(rate.getRemaining() <= 0){
			ctx.put("error.status_code", 429);
		}
		return null;
	}
	private Policy findRequestPolicy(HttpServletRequest request){
		return properties.getPolicies().get(RateLimitProperties.PolicyType.ANONYMOUS);
	}

	private String findKey(HttpServletRequest request){
		return request.getRemoteAddr();
	}
}
