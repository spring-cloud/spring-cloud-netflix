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

package org.springframework.cloud.netflix.zuul.routematcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.netflix.zuul.filters.RefreshableRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.SimpleRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;
import org.springframework.cloud.netflix.zuul.util.RequestUtils;
import org.springframework.core.Ordered;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UrlPathHelper;

/**
 * AdvancedRouteMatcher {@link RouteMatcher} based on configuration data held in {@link ZuulProperties}.
 * 
 * This is mainly copied from {@link SimpleRouteLocator} and following the same pattern but instead of
 * using String for matching, now using HttpServletRequest object to allow fine-grained matching.
 *
 * @author Dave Syer
 * @author Mustansar Anwar
 */
public class AdvancedRouteMatcher implements RouteMatcher, Ordered {

	private static final Log log = LogFactory
			.getLog(AdvancedRouteMatcher.class);

	private static final int DEFAULT_ORDER = 0;

	private int order = DEFAULT_ORDER;

	private String dispatcherServletPath = "/";
	private String zuulServletPath;
	private ZuulProperties properties;
	private AlternateRouteLookup alternateRouteLookup;

	private PathMatcher pathMatcher = new AntPathMatcher();
	private UrlPathHelper urlPathHelper = new UrlPathHelper();

	
	private AtomicReference<Map<String, ZuulRoute>> routes = new AtomicReference<>();

	public AdvancedRouteMatcher(String servletPath, ZuulProperties properties,
			AlternateRouteLookup alternateRouteLookup) {
		this.properties = properties;
		if (StringUtils.hasText(servletPath)) {
			this.dispatcherServletPath = servletPath;
		}

		this.zuulServletPath = properties.getServletPath();
		this.urlPathHelper.setRemoveSemicolonContent(
				properties.isRemoveSemicolonContent());
		this.alternateRouteLookup = alternateRouteLookup;
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
	public Route getMatchingRoute(HttpServletRequest request) {

		final String requestURI = this.urlPathHelper
				.getPathWithinApplication(request);
		if (log.isDebugEnabled()) {
			log.debug("Finding route for path: " + requestURI);
		}

		if (this.routes.get() == null) {
			this.routes.set(locateRoutes());
		}

		if (log.isDebugEnabled()) {
			log.debug("servletPath=" + this.dispatcherServletPath);
			log.debug("zuulServletPath=" + this.zuulServletPath);
			log.debug("RequestUtils.isDispatcherServletRequest()="
					+ RequestUtils.isDispatcherServletRequest());
			log.debug("RequestUtils.isZuulServletRequest()="
					+ RequestUtils.isZuulServletRequest());
		}

		String adjustedPath = adjustPath(requestURI);
		ZuulRoute route = getZuulRoute(adjustedPath, request);

		route = checkMethodAllowed(route, request);

		return buildRoute(route, adjustedPath, request);
	}

	private ZuulRoute checkMethodAllowed(ZuulRoute route,
			HttpServletRequest request) {
		if (route == null) {
			return null;
		}
		RouteOptions routeOption = route.getRouteOptions();
		String requestMethod = request.getMethod();

		if (routeOption != null && !routeOption.getAllowedMethods().isEmpty()
				&& !routeOption.getAllowedMethods().contains(requestMethod)) {
			return null;
		}
		return route;
	}

	protected Route buildRoute(ZuulRoute route, String path,
			HttpServletRequest request) {
		if (route == null) {
			return null;
		}
		if (log.isDebugEnabled()) {
			log.debug("route matched=" + route);
		}
		// Set a default location for route
		String location = route.getLocation();

		AlternateRoute alternateRoute = alternateRouteLookup.lookupAlternateRoute(route, request);

		if (alternateRoute != null) {
			location = alternateRoute.getUri().toString();
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
		return new Route(route.getId(), targetPath, location, prefix,
				retryable, route.isCustomSensitiveHeaders()
						? route.getSensitiveHeaders() : null,
				route.getRouteOptions(), route.isStripPrefix());
	}

	protected ZuulRoute getZuulRoute(String adjustedPath,
			HttpServletRequest request) {
		if (!matchesIgnoredPatterns(adjustedPath)) {
			for (Entry<String, ZuulRoute> entry : this.routes.get()
					.entrySet()) {
				String pattern = entry.getKey();
				log.debug("Matching pattern:" + pattern);
				ZuulRoute zuulRoute = this.matchRoute(adjustedPath, entry,
						request);
				if (zuulRoute != null) {
					return zuulRoute;
				}
			}
		}
		return null;
	}

	protected ZuulRoute matchRoute(String path, Entry<String, ZuulRoute> entry,
			HttpServletRequest request) {
		String pattern = entry.getKey();
		ZuulRoute route = entry.getValue();
		if (this.pathMatcher.match(pattern, path)) {
			return route;
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
				retryable, route.isCustomSensitiveHeaders()
						? route.getSensitiveHeaders() : null,
				route.getRouteOptions(), route.isStripPrefix());
	}

	/**
	 * Calculate all the routes and set up a cache for the values. Subclasses can call this method if
	 * they need to implement {@link RefreshableRouteLocator}.
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
				log.debug(
						"Path " + path + " matches ignored pattern " + pattern);
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
				adjustedPath = path
						.substring(this.dispatcherServletPath.length());
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

	@Override
	public Route getMatchingRoute(String path) {
		throw new RuntimeException("Encountered unreachable code flow.");
	}

}
