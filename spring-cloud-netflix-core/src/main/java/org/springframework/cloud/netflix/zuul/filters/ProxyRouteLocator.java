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

package org.springframework.cloud.netflix.zuul.filters;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;

import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * @author Spencer Gibb
 */
@CommonsLog
public class ProxyRouteLocator implements RouteLocator {

	public static final String DEFAULT_ROUTE = "/**";

	private DiscoveryClient discovery;

	private ZuulProperties properties;

	private PathMatcher pathMatcher = new AntPathMatcher();

	private AtomicReference<Map<String, ZuulRoute>> routes = new AtomicReference<>();

	private Map<String, ZuulRoute> staticRoutes = new LinkedHashMap<>();

	private String servletPath;

	public ProxyRouteLocator(String servletPath, DiscoveryClient discovery,
			ZuulProperties properties) {
		this.servletPath = servletPath;
		this.discovery = discovery;
		this.properties = properties;
	}

	public void addRoute(String path, String location) {
		this.staticRoutes.put(path, new ZuulRoute(path, location));
		resetRoutes();
	}

	public void addRoute(ZuulRoute route) {
		this.staticRoutes.put(route.getPath(), route);
		resetRoutes();
	}

	@Override
	public Collection<String> getRoutePaths() {
		return getRoutes().keySet();
	}

	public Map<String, String> getRoutes() {
		if (this.routes.get() == null) {
			this.routes.set(locateRoutes());
		}
		Map<String, String> values = new LinkedHashMap<>();
		for (String key : this.routes.get().keySet()) {
			String url = key;
			values.put(url, this.routes.get().get(key).getLocation());
		}
		return values;
	}

	public ProxyRouteSpec getMatchingRoute(String path) {
		String location = null;
		String targetPath = null;
		String id = null;
		String prefix = this.properties.getPrefix();
		if (StringUtils.hasText(this.servletPath) && !this.servletPath.equals("/")
				&& path.startsWith(this.servletPath)) {
			path = path.substring(this.servletPath.length());
		}
		Boolean retryable = this.properties.getRetryable();
		for (Entry<String, ZuulRoute> entry : this.routes.get().entrySet()) {
			String pattern = entry.getKey();
			if (this.pathMatcher.match(pattern, path)) {
				ZuulRoute route = entry.getValue();
				id = route.getId();
				location = route.getLocation();
				targetPath = path;
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
				if (route.getRetryable() != null) {
					retryable = route.getRetryable();
				}
				break;
			}
		}
		return (location == null ? null : new ProxyRouteSpec(id, targetPath, location,
				prefix, retryable));
	}

	public void resetRoutes() {
		this.routes.set(locateRoutes());
	}

	protected LinkedHashMap<String, ZuulRoute> locateRoutes() {
		LinkedHashMap<String, ZuulRoute> routesMap = new LinkedHashMap<String, ZuulRoute>();
		addConfiguredRoutes(routesMap);
		routesMap.putAll(this.staticRoutes);
		if (this.discovery != null) {
			Map<String, ZuulRoute> staticServices = new LinkedHashMap<String, ZuulRoute>();
			for (ZuulRoute route : routesMap.values()) {
				String serviceId = route.getServiceId();
				if (serviceId == null) {
					serviceId = route.getId();
				}
				if (serviceId != null) {
					staticServices.put(serviceId, route);
				}
			}
			// Add routes for discovery services by default
			List<String> services = this.discovery.getServices();
			String[] ignored = this.properties.getIgnoredServices()
					.toArray(new String[0]);
			for (String serviceId : services) {
				// Ignore specifically ignored services and those that were manually
				// configured
				String key = "/" + serviceId + "/**";
				if (staticServices.containsKey(serviceId)
						&& staticServices.get(serviceId).getUrl() == null) {
					// Explicitly configured with no URL, cannot be ignored
					// all static routes are already in routesMap, just update
					ZuulRoute staticRoute = staticServices.get(serviceId);
					staticRoute.updateRoute(key, serviceId);
				}
				if (!PatternMatchUtils.simpleMatch(ignored, serviceId)
						&& !routesMap.containsKey(key)) {
					// Not ignored
					routesMap.put(key, new ZuulRoute(key, serviceId));
				}
			}
		}
		if (routesMap.get(DEFAULT_ROUTE) != null) {
			ZuulRoute defaultRoute = routesMap.get(DEFAULT_ROUTE);
			// Move the defaultServiceId to the end
			routesMap.remove(DEFAULT_ROUTE);
			routesMap.put(DEFAULT_ROUTE, defaultRoute);
		}
		LinkedHashMap<String, ZuulRoute> values = new LinkedHashMap<>();
		for (Entry<String, ZuulRoute> entry : routesMap.entrySet()) {
			String path = entry.getKey();
			// Prepend with slash if not already present.
			if (!path.startsWith("/")) {
				path = "/" + path;
			}
			if (StringUtils.hasText(this.properties.getPrefix())) {
				path = this.properties.getPrefix() + path;
				if (!path.startsWith("/")) {
					path = "/" + path;
				}
			}
			values.put(path, entry.getValue());
		}
		return values;
	}

	protected void addConfiguredRoutes(Map<String, ZuulRoute> routes) {
		Map<String, ZuulRoute> routeEntries = this.properties.getRoutes();
		for (ZuulRoute entry : routeEntries.values()) {
			String route = entry.getPath();
			if (routes.containsKey(route)) {
				log.warn("Overwriting route " + route + ": already defined by "
						+ routes.get(route));
			}
			routes.put(route, entry);
		}
	}

	public String getTargetPath(String matchingRoute, String requestURI) {
		String path = getRoutes().get(matchingRoute);
		return (path != null ? path : requestURI);

	}

	@Data
	@AllArgsConstructor
	public static class ProxyRouteSpec {

		private String id;

		private String path;

		private String location;

		private String prefix;

		private Boolean retryable;

	}

}
