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

import javax.servlet.RequestDispatcher;

import org.springframework.util.ReflectionUtils;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

/**
 * @author Dave Syer
 */
public class SendForwardFilter extends ZuulFilter {

	protected static final String SEND_FORWARD_FILTER_RAN = "sendForwardFilter.ran";

	@Override
	public String filterType() {
		return "post";
	}

	@Override
	public int filterOrder() {
		return 2000;
	}

	@Override
	public boolean shouldFilter() {
		RequestContext ctx = RequestContext.getCurrentContext();
		return ctx.containsKey("forward.to")
				&& !ctx.getBoolean(SEND_FORWARD_FILTER_RAN, false);
	}

	@Override
	public Object run() {
		try {
			RequestContext ctx = RequestContext.getCurrentContext();
			String path = (String) ctx.get("forward.to");
			RequestDispatcher dispatcher = ctx.getRequest().getRequestDispatcher(path);
			if (dispatcher != null) {
				ctx.set(SEND_FORWARD_FILTER_RAN, true);
				if (!ctx.getResponse().isCommitted()) {
					dispatcher.forward(ctx.getRequest(), ctx.getResponse());
				}
			}
		}
		catch (Exception ex) {
			ReflectionUtils.rethrowRuntimeException(ex);
		}
		return null;
	}

}
