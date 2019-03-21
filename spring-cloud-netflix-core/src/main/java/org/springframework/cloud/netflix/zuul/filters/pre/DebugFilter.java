/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.zuul.filters.pre;

import javax.servlet.http.HttpServletRequest;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.constants.ZuulConstants;
import com.netflix.zuul.context.RequestContext;

/**
 * @author Spencer Gibb
 */
public class DebugFilter extends ZuulFilter {

	private static final DynamicBooleanProperty ROUTING_DEBUG = DynamicPropertyFactory
			.getInstance().getBooleanProperty(ZuulConstants.ZUUL_DEBUG_REQUEST, false);

	private static final DynamicStringProperty DEBUG_PARAMETER = DynamicPropertyFactory
			.getInstance().getStringProperty(ZuulConstants.ZUUL_DEBUG_PARAMETER, "debug");

	@Override
	public String filterType() {
		return "pre";
	}

	@Override
	public int filterOrder() {
		return 1;
	}

	@Override
	public boolean shouldFilter() {
		HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
		if ("true".equals(request.getParameter(DEBUG_PARAMETER.get()))) {
			return true;
		}
		return ROUTING_DEBUG.get();
	}

	@Override
	public Object run() {
		RequestContext ctx = RequestContext.getCurrentContext();
		ctx.setDebugRouting(true);
		ctx.setDebugRequest(true);
		return null;
	}

}
