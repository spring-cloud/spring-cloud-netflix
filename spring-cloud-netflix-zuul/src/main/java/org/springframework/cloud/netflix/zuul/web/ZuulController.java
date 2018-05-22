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

package org.springframework.cloud.netflix.zuul.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ServletWrappingController;

import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.http.ZuulServlet;

/**
 * @author Spencer Gibb
 */
public class ZuulController extends ServletWrappingController {

	public ZuulController() {
		setServletClass(ZuulServlet.class);
		setServletName("zuul");
		setSupportedMethods((String[]) null); // Allow all
	}

	@Override
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		try {
			// We don't care about the other features of the base class, just want to
			// handle the request
			return super.handleRequestInternal(request, response);
		}
		finally {
			// @see com.netflix.zuul.context.ContextLifecycleFilter.doFilter
			RequestContext.getCurrentContext().unset();
		}
	}

}
