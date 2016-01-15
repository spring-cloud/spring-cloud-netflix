/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.cloud.netflix.zuul.filters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;

import lombok.extern.apachecommons.CommonsLog;

/**
 * Simple {@link RouteLocator} based on configuration data held in {@link ZuulProperties}.
 *
 * @author Dave Syer
 */
@CommonsLog
public class SimpleRouteLocator implements RouteLocator {

	private ZuulProperties properties;

	private PathMatcher pathMatcher = new AntPathMatcher();

	private String servletPath;

	private AtomicReference<Map<String, ZuulRoute>> routes = new AtomicReference<>();

	public SimpleRouteLocator(String servletPath, ZuulProperties properties) {
		this.properties = properties;
		if (StringUtils.hasText(servletPath)) { // a servletPath is passed explicitly
			this.servletPath = servletPath;
		}
		else {
			// set Zuul servlet path
			this.servletPath = properties.getServletPath() != null
					? properties.getServletPath() : "";
		}
	}

	@Override
	public List<Route> getRoutes() {
		if (this.routes.get() == null) {
			this.routes.set(locateRoutes());
		}
		List<Route> values = new ArrayList<>();
		for (String url : this.routes.get().keySet()) {
			ZuulRoute route = this.routes.get().get(url);
			String path = route.getPath();
			values.add(getRoute(route, path));
		}
		return values;
	}

	@Override
	public Collection<String> getIgnoredPaths() {
		return this.properties.getIgnoredPatterns();
	}

	@Override
	public Route getMatchingRoute(String path) {

		if (log.isDebugEnabled()) {
			log.debug("Finding route for path: " + path);
		}

		if (this.routes.get() == null) {
			this.routes.set(locateRoutes());
		}

		log.debug("servletPath=" + this.servletPath);
		if (StringUtils.hasText(this.servletPath) && !this.servletPath.equals("/")
				&& path.startsWith(this.servletPath)) {
			path = path.substring(this.servletPath.length());
		}
		log.debug("path=" + path);
		ZuulRoute route = null;
		if (!matchesIgnoredPatterns(path)) {
			for (Entry<String, ZuulRoute> entry : this.routes.get().entrySet()) {
				String pattern = entry.getKey();
				log.debug("Matching pattern:" + pattern);
				if (this.pathMatcher.match(pattern, path)) {
					route = entry.getValue();
					break;
				}
			}
		}

		return getRoute(route, path);

	}

	private Route getRoute(ZuulRoute route, String path) {
		if (route == null) {
			return null;
		}
		String targetPath = path;
		String prefix = this.properties.getPrefix();
		if (path.startsWith(prefix) && this.properties.isStripPrefix()) {
			targetPath = path.substring(prefix.length());
		}
		if (route.isStripPrefix()) {
			int index = route.getPath().indexOf("*") - 1;
			if (index > 0) {
				String routePrefix = route.getPath().substring(0, index);
				targetPath = targetPath.replaceFirst(routePrefix, "");
				prefix = prefix + routePrefix;
			}
		}
		Boolean retryable = this.properties.getRetryable();
		if (route.getRetryable() != null) {
			retryable = route.getRetryable();
		}
		return new Route(route.getId(), targetPath, route.getLocation(), prefix,
				retryable);
	}

	/**
	 * Calculate all the routes and set up a cache for the values. Subclasses can call
	 * this method if they need to implement {@link RefreshableRouteLocator}.
	 */
	protected void doRefresh() {
		this.routes.set(locateRoutes());
	}

	/**
	 * Compute a map of path pattern to route. The default is just a static map from the
	 * {@link ZuulProperties}, but subclasses can add dynamic calculations.
	 */
	protected Map<String, ZuulRoute> locateRoutes() {
		LinkedHashMap<String, ZuulRoute> routesMap = new LinkedHashMap<String, ZuulRoute>();
		for (ZuulRoute route : this.properties.getRoutes().values()) {
			routesMap.put(route.getPath(), route);
		}
		return routesMap;
	}

	protected boolean matchesIgnoredPatterns(String path) {
		for (String pattern : this.properties.getIgnoredPatterns()) {
			log.debug("Matching ignored pattern:" + pattern);
			if (this.pathMatcher.match(pattern, path)) {
				log.debug("Path " + path + " matches ignored pattern " + pattern);
				return true;
			}
		}
		return false;
	}

}
