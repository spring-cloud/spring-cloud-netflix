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

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.routematcher.RouteMatcher;

/**
 * Use {@link RouteMatcher} instead of {@link RouteMatcher}.
 * 
 * @author Mustansar Anwar
 * 
 */
public class PreDecorationRouteMatcherFilter extends PreDecorationFilter {

	private static final Log log = LogFactory
			.getLog(PreDecorationRouteMatcherFilter.class);

	private RouteMatcher routeMatcher;

	public PreDecorationRouteMatcherFilter(String dispatcherServletPath,
			ZuulProperties properties, ProxyRequestHelper proxyRequestHelper,
			RouteMatcher routeMatcher) {
		super(dispatcherServletPath, properties, proxyRequestHelper);
		this.routeMatcher = routeMatcher;
	}

	@Override
	protected Route getMatchingRoute(HttpServletRequest request) {
		return this.routeMatcher.getMatchingRoute(request);
	}
}
