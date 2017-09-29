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

package org.springframework.cloud.netflix.zuul.filters.pre;

import java.lang.reflect.Field;

import javax.servlet.http.HttpServletRequest;

import org.springframework.cloud.netflix.zuul.util.RequestUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.http.HttpServletRequestWrapper;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVLET_30_WRAPPER_FILTER_ORDER;

/**
 * Pre {@link ZuulFilter} that wraps requests in a Servlet 3.0 compliant wrapper.
 * Zuul's default wrapper is only Servlet 2.5 compliant.
 * @author Spencer Gibb
 */
public class Servlet30WrapperFilter extends ZuulFilter {

	private Field requestField = null;

	public Servlet30WrapperFilter() {
		this.requestField = ReflectionUtils.findField(HttpServletRequestWrapper.class,
				"req", HttpServletRequest.class);
		Assert.notNull(this.requestField,
				"HttpServletRequestWrapper.req field not found");
		this.requestField.setAccessible(true);
	}

	protected Field getRequestField() {
		return this.requestField;
	}

	@Override
	public String filterType() {
		return PRE_TYPE;
	}

	@Override
	public int filterOrder() {
		return SERVLET_30_WRAPPER_FILTER_ORDER;
	}

	@Override
	public boolean shouldFilter() {
		return true; // TODO: only if in servlet 3.0 env
	}

	@Override
	public Object run() {
		RequestContext ctx = RequestContext.getCurrentContext();
		HttpServletRequest request = ctx.getRequest();
		if (request instanceof HttpServletRequestWrapper) {
			request = (HttpServletRequest) ReflectionUtils.getField(this.requestField,
					request);
			ctx.setRequest(new Servlet30RequestWrapper(request));
		}
		else if (RequestUtils.isDispatcherServletRequest()) {
			// If it's going through the dispatcher we need to buffer the body
			ctx.setRequest(new Servlet30RequestWrapper(request));
		}
		return null;
	}

}
