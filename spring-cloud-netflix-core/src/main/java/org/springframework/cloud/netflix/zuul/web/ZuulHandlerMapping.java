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

package org.springframework.cloud.netflix.zuul.web;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.cloud.netflix.zuul.filters.RefreshableRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping;

import com.netflix.zuul.context.RequestContext;

/**
 * MVC HandlerMapping that maps incoming request paths to remote services.
 *
 * @author Spencer Gibb
 * @author Dave Syer
 * @author João Salavessa
 * @author Biju Kunjummen
 */
public class ZuulHandlerMapping extends AbstractUrlHandlerMapping {

	private final RouteLocator routeLocator;

	private final ZuulController zuul;

	private ErrorController errorController;

	private PathMatcher pathMatcher = new AntPathMatcher();

	private volatile boolean dirty = true;

	public ZuulHandlerMapping(RouteLocator routeLocator, ZuulController zuul) {
		this.routeLocator = routeLocator;
		this.zuul = zuul;
		setOrder(-200);
	}

	@Override
	protected HandlerExecutionChain getCorsHandlerExecutionChain(HttpServletRequest request,
			HandlerExecutionChain chain, CorsConfiguration config) {
		if (config == null) {
			// Allow CORS requests to go to the backend
			return chain;
		}
		return super.getCorsHandlerExecutionChain(request, chain, config);
	}

	public void setErrorController(ErrorController errorController) {
		this.errorController = errorController;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
		if (this.routeLocator instanceof RefreshableRouteLocator) {
			((RefreshableRouteLocator) this.routeLocator).refresh();
		}
	}

	@Override
	protected Object lookupHandler(String urlPath, HttpServletRequest request) throws Exception {
		if (this.errorController != null && urlPath.equals(this.errorController.getErrorPath())) {
			return null;
		}
		if (isIgnoredPath(urlPath, this.routeLocator.getIgnoredPaths())) return null;
		RequestContext ctx = RequestContext.getCurrentContext();
		if (ctx.containsKey("forward.to")) {
			return null;
		}
		if (this.dirty) {
			synchronized (this) {
				if (this.dirty) {
					registerHandlers();
					this.dirty = false;
				}
			}
		}
		return super.lookupHandler(urlPath, request);
	}

	private boolean isIgnoredPath(String urlPath, Collection<String> ignored) {
		if (ignored != null) {
			for (String ignoredPath : ignored) {
				if (this.pathMatcher.match(ignoredPath, urlPath)) {
					return true;
				}
			}
		}
		return false;
	}

	private void registerHandlers() {
		Collection<Route> routes = this.routeLocator.getRoutes();
		if (routes.isEmpty()) {
			this.logger.warn("No routes found from RouteLocator");
		}
		else {
			for (Route route : routes) {
				registerHandler(route.getFullPath(), this.zuul);
			}
		}
	}

}
