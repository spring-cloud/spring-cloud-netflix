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

import javax.servlet.RequestDispatcher;

import org.springframework.util.ReflectionUtils;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.FORWARD_TO_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.ROUTE_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SEND_FORWARD_FILTER_ORDER;

/**
 * Route {@link ZuulFilter} that forwards requests using the {@link RequestDispatcher}.
 * Forwarding location is located in the {@link RequestContext} attribute {@link org.springframework.cloud.netflix.zuul.filters.support.FilterConstants#FORWARD_TO_KEY}.
 * Useful for forwarding to endpoints in the current application.
 *
 * @author Dave Syer
 */
public class SendForwardFilter extends ZuulFilter {

	protected static final String SEND_FORWARD_FILTER_RAN = "sendForwardFilter.ran";

	@Override
	public String filterType() {
		return ROUTE_TYPE;
	}

	@Override
	public int filterOrder() {
		return SEND_FORWARD_FILTER_ORDER;
	}

	@Override
	public boolean shouldFilter() {
		RequestContext ctx = RequestContext.getCurrentContext();
		return ctx.containsKey(FORWARD_TO_KEY)
				&& !ctx.getBoolean(SEND_FORWARD_FILTER_RAN, false);
	}

	@Override
	public Object run() {
		try {
			RequestContext ctx = RequestContext.getCurrentContext();
			String path = (String) ctx.get(FORWARD_TO_KEY);
			RequestDispatcher dispatcher = ctx.getRequest().getRequestDispatcher(path);
			if (dispatcher != null) {
				ctx.set(SEND_FORWARD_FILTER_RAN, true);
				if (!ctx.getResponse().isCommitted()) {
					dispatcher.forward(ctx.getRequest(), ctx.getResponse());
					ctx.getResponse().flushBuffer();
				}
			}
		}
		catch (Exception ex) {
			ReflectionUtils.rethrowRuntimeException(ex);
		}
		return null;
	}

}
