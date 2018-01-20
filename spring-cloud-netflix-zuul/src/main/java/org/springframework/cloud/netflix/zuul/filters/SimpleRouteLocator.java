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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;
import org.springframework.cloud.netflix.zuul.util.RequestUtils;
import org.springframework.core.Ordered;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;

/**
 * Simple {@link RouteLocator} based on configuration data held in {@link ZuulProperties}.
 *
 * @author Dave Syer
 */
public class SimpleRouteLocator implements RouteLocator, Ordered {

	private static final Log log = LogFactory.getLog(SimpleRouteLocator.class);

	private static final int DEFAULT_ORDER = 0;

	private ZuulProperties properties;

	private PathMatcher pathMatcher = new AntPathMatcher();

	private String dispatcherServletPath = "/";
	private String zuulServletPath;

	private AtomicReference<Map<String, ZuulRoute>> routes = new AtomicReference<>();
	private int order = DEFAULT_ORDER;

	public SimpleRouteLocator(String servletPath, ZuulProperties properties) {
		this.properties = properties;
		if (StringUtils.hasText(servletPath)) {
			this.dispatcherServletPath = servletPath;
		}

		this.zuulServletPath = properties.getServletPath();
	}

	@Override
	public List<Route> getRoutes() {
		List<Route> values = new ArrayList<>();
		for (Entry<String, ZuulRoute> entry : getRoutesMap().entrySet()) {
			ZuulRoute route = entry.getValue();
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
	public Route getMatchingRoute(final String path) {

		return getSimpleMatchingRoute(path);

	}

	protected Map<String, ZuulRoute> getRoutesMap() {
		if (this.routes.get() == null) {
			this.routes.set(locateRoutes());
		}
		return this.routes.get();
	}

	protected Route getSimpleMatchingRoute(final String path) {
		if (log.isDebugEnabled()) {
			log.debug("Finding route for path: " + path);
		}

		// This is called for the initialization done in getRoutesMap()
		getRoutesMap();

		if (log.isDebugEnabled()) {
			log.debug("servletPath=" + this.dispatcherServletPath);
			log.debug("zuulServletPath=" + this.zuulServletPath);
			log.debug("RequestUtils.isDispatcherServletRequest()="
					+ RequestUtils.isDispatcherServletRequest());
			log.debug("RequestUtils.isZuulServletRequest()="
					+ RequestUtils.isZuulServletRequest());
		}

		String adjustedPath = adjustPath(path);

		ZuulRoute route = getZuulRoute(adjustedPath);

		return getRoute(route, adjustedPath);
	}

	protected ZuulRoute getZuulRoute(String adjustedPath) {
		if (!matchesIgnoredPatterns(adjustedPath)) {
			for (Entry<String, ZuulRoute> entry : getRoutesMap().entrySet()) {
				String pattern = entry.getKey();
				log.debug("Matching pattern:" + pattern);
				if (this.pathMatcher.match(pattern, adjustedPath)) {
					return entry.getValue();
				}
			}
		}
		return null;
	}

	protected Route getRoute(ZuulRoute route, String path) {
		if (route == null) {
			return null;
		}
		if (log.isDebugEnabled()) {
			log.debug("route matched=" + route);
		}
		String targetPath = path;
		String prefix = this.properties.getPrefix();
		if(prefix.endsWith("/")) {
			prefix = prefix.substring(0, prefix.length() - 1);
		}
		if (path.startsWith(prefix + "/") && this.properties.isStripPrefix()) {
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
				retryable,
				route.isCustomSensitiveHeaders() ? route.getSensitiveHeaders() : null, 
				route.isStripPrefix());
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

	private String adjustPath(final String path) {
		String adjustedPath = path;

		if (RequestUtils.isDispatcherServletRequest()
				&& StringUtils.hasText(this.dispatcherServletPath)) {
			if (!this.dispatcherServletPath.equals("/")) {
				adjustedPath = path.substring(this.dispatcherServletPath.length());
				log.debug("Stripped dispatcherServletPath");
			}
		}
		else if (RequestUtils.isZuulServletRequest()) {
			if (StringUtils.hasText(this.zuulServletPath)
					&& !this.zuulServletPath.equals("/")) {
				adjustedPath = path.substring(this.zuulServletPath.length());
				log.debug("Stripped zuulServletPath");
			}
		}
		else {
			// do nothing
		}

		log.debug("adjustedPath=" + adjustedPath);
		return adjustedPath;
	}

	@Override
	public int getOrder() {
		return order;
	}
	
	public void setOrder(int order) {
		this.order = order;
	}

}
