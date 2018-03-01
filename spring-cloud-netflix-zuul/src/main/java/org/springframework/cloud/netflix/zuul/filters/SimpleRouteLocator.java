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

import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;
import org.springframework.cloud.netflix.zuul.util.RequestUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;

/**
 * Simple {@link RouteLocator} based on configuration data held in {@link ZuulProperties}.
 *
 * @author Dave Syer
 * @author Arnold Galovics
 */
public class SimpleRouteLocator implements RouteLocator, Ordered {

	private static final Log log = LogFactory.getLog(SimpleRouteLocator.class);

	private static final int DEFAULT_ORDER = 0;

	private ZuulProperties properties;

	private PathMatcher pathMatcher = new AntPathMatcher();

	private String dispatcherServletPath = "/";
	private String zuulServletPath;

	private AtomicReference<Map<String, Map<HttpMethod, ZuulRoute>>> routes = new AtomicReference<>();
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
		Set<ZuulRoute> routes = getRoutesMap().values().stream().map(Map::values).flatMap(Collection::stream).collect(toSet());
		for (ZuulRoute route : routes) {
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
	public Route getMatchingRoute(RequestWrapper request) {
		return getSimpleMatchingRoute(request);
	}

	protected Map<String, Map<HttpMethod, ZuulRoute>> getRoutesMap() {
		if (this.routes.get() == null) {
			this.routes.set(locateRoutes());
		}
		return this.routes.get();
	}

	protected Route getSimpleMatchingRoute(final RequestWrapper request) {
		if (log.isDebugEnabled()) {
			log.debug("Finding route for request: " + request);
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

		String adjustedPath = adjustPath(request.getPath());

		ZuulRoute route = getZuulRoute(adjustedPath, request.getMethod());

		return getRoute(route, adjustedPath);
	}

	protected ZuulRoute getZuulRoute(String adjustedPath, HttpMethod method) {
		if (!matchesIgnoredPatterns(adjustedPath)) {
			for (Entry<String, Map<HttpMethod, ZuulRoute>> entry : getRoutesMap().entrySet()) {
				String pattern = entry.getKey();
				log.debug("Matching pattern:" + pattern);
				if (this.pathMatcher.match(pattern, adjustedPath)) {
					Map<HttpMethod, ZuulRoute> methods = entry.getValue();
					return methods.entrySet().stream().filter(e -> matchesMethod(method, e)).findAny().map(Entry::getValue).orElse(null);
				}
			}
		}
		return null;
	}

	private boolean matchesMethod(HttpMethod method, Entry<HttpMethod, ZuulRoute> zuulRoute) {
		return method.equals(zuulRoute.getKey());
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
				route.isStripPrefix(), route.getMethods());
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
	protected Map<String, Map<HttpMethod, ZuulRoute>> locateRoutes() {
		LinkedHashMap<String, Map<HttpMethod, ZuulRoute>> routesMap = new LinkedHashMap<>();
		for (ZuulRoute route : this.properties.getRoutes().values()) {
			String path = route.getPath();
			Set<HttpMethod> methods = route.getMethods();
			if (CollectionUtils.isEmpty(methods)) {
				methods = Arrays.stream(HttpMethod.values()).collect(toSet());
			}
			Map<HttpMethod, ZuulRoute> routeMap = methods.stream().collect(toMap(identity(), method -> route));
			Map<HttpMethod, ZuulRoute> existingMappings = routesMap.get(path);
			if (existingMappings != null) {
				existingMappings.entrySet().forEach(e -> {
					HttpMethod existingMethod = e.getKey();
					ZuulRoute existingRoute = e.getValue();
					if (!routeMap.containsKey(existingMethod)) {
						routeMap.put(existingMethod, existingRoute);
					} else {
						log.warn(format("Duplicated route definition found for path: %s and method: %s", path, existingMethod));
					}
				});
			}
			routesMap.put(path, routeMap);
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
