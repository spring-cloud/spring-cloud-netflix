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

package org.springframework.cloud.netflix.zuul.web;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping;

/**
 * MVC HandlerMapping that maps incoming request paths to remote services.
 *
 * @author Spencer Gibb
 * @author Dave Syer
 */
public class ZuulHandlerMapping extends AbstractUrlHandlerMapping {

	private final RouteLocator routeLocator;

	private final ZuulController zuul;

	private ErrorController errorController;

	public ZuulHandlerMapping(RouteLocator routeLocator, ZuulController zuul) {
		this.routeLocator = routeLocator;
		this.zuul = zuul;
		setOrder(-200);
	}

	public void setErrorController(ErrorController errorController) {
		this.errorController = errorController;
	}

	@Override
	protected Object lookupHandler(String urlPath, HttpServletRequest request)
			throws Exception {
		if (this.errorController != null
				&& urlPath.equals(this.errorController.getErrorPath())) {
			return null;
		}
		return super.lookupHandler(urlPath, request);
	}

	public void registerHandlers() {
		Collection<String> routes = this.routeLocator.getRoutePaths();
		if (routes.isEmpty()) {
			this.logger.warn("No routes found from ProxyRouteLocator");
		}
		else {
			for (String url : routes) {
				registerHandler(url, this.zuul);
			}
		}
	}

}
