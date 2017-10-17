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

package org.springframework.cloud.netflix.zuul.util;

import com.netflix.zuul.context.RequestContext;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.IS_DISPATCHER_SERVLET_REQUEST_KEY;

public class RequestUtils {

	/**
	 * @deprecated use {@link org.springframework.cloud.netflix.zuul.filters.support.FilterConstants#IS_DISPATCHER_SERVLET_REQUEST_KEY}
	 */
	@Deprecated
	public static final String IS_DISPATCHERSERVLETREQUEST = IS_DISPATCHER_SERVLET_REQUEST_KEY;
	
	public static boolean isDispatcherServletRequest() {
		return RequestContext.getCurrentContext().getBoolean(IS_DISPATCHER_SERVLET_REQUEST_KEY);
	}
	
	public static boolean isZuulServletRequest() {
		//extra check for dispatcher since ZuulServlet can run from ZuulController
		return !isDispatcherServletRequest() && RequestContext.getCurrentContext().getZuulEngineRan();
	}	
}
