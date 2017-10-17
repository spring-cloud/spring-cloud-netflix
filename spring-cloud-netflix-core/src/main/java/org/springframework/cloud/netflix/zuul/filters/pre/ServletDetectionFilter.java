/*
 * Copyright 2013-2017 the original author or authors.
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

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.DispatcherServlet;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.http.HttpServletRequestWrapper;
import com.netflix.zuul.http.ZuulServlet;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.IS_DISPATCHER_SERVLET_REQUEST_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVLET_DETECTION_FILTER_ORDER;

/**
 * Detects whether a request is ran through the {@link DispatcherServlet} or {@link ZuulServlet}.
 * The purpose was to detect this up-front at the very beginning of Zuul filter processing
 *  and rely on this information in all filters.
 *  RequestContext is used such that the information is accessible to classes 
 *  which do not have a request reference.
 * @author Adrian Ivan
 */
public class ServletDetectionFilter extends ZuulFilter {

	public ServletDetectionFilter() {
	}

	@Override
	public String filterType() {
		return PRE_TYPE;
	}

	/**
	 * Must run before other filters that rely on the difference between 
	 * DispatcherServlet and ZuulServlet.
	 */
	@Override
	public int filterOrder() {
		return SERVLET_DETECTION_FILTER_ORDER;
	}

	@Override
	public boolean shouldFilter() {
		return true; 
	}

	@Override
	public Object run() {
		RequestContext ctx = RequestContext.getCurrentContext();
		HttpServletRequest request = ctx.getRequest();
		if (!(request instanceof HttpServletRequestWrapper) 
				&& isDispatcherServletRequest(request)) {
			ctx.set(IS_DISPATCHER_SERVLET_REQUEST_KEY, true);
		} else {
			ctx.set(IS_DISPATCHER_SERVLET_REQUEST_KEY, false);
		}

		return null;
	}
	
	private boolean isDispatcherServletRequest(HttpServletRequest request) {
		return request.getAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE) != null;
	}	 	

}
