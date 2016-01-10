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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Spencer Gibb
 */
@CommonsLog
public class ProxyRouteLocator implements RouteLocator {

	public static final String DEFAULT_ROUTE = "/**";

	private final DiscoveryClient discovery;

	private final ZuulProperties properties;

	private final PathMatcher pathMatcher = new AntPathMatcher();

	private final Map<String, ZuulRoute> staticRoutes = new LinkedHashMap<>();

	private final String servletPath;

	private ServiceRouteMapper serviceRouteMapper;

	private final ZuulRouteMatcher routeMatcher;

	public ProxyRouteLocator(String servletPath, DiscoveryClient discovery,
			ZuulProperties properties, ZuulRouteMatcher routeMatcher) {
		String path = servletPath;
		if (!StringUtils.hasText(path)) {
			// set Zuul servlet path
			path = properties.getServletPath() != null
					? properties.getServletPath() : "";
		}
		this.servletPath = path;

		if (properties.isIgnoreLocalService()) {
			ServiceInstance instance = discovery.getLocalServiceInstance();
			if (instance != null) {
				String localServiceId = instance.getServiceId();
				if (!properties.getIgnoredServices().contains(localServiceId)) {
					properties.getIgnoredServices().add(localServiceId);
				}
			}
		}
		this.serviceRouteMapper = new SimpleServiceRouteMapper();
		this.discovery = discovery;
		this.properties = properties;
		this.routeMatcher = routeMatcher;
	}

	public ProxyRouteLocator(String servletPath, DiscoveryClient discovery,
							 ZuulProperties properties, ZuulRouteMatcher routeMatcher, ServiceRouteMapper serviceRouteMapper) {
		this(servletPath, discovery, properties, routeMatcher);
		this.serviceRouteMapper = serviceRouteMapper;
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

	@Override
	public Collection<String> getIgnoredPaths() {
		return this.properties.getIgnoredPatterns();
	}

	public Map<String, String> getRoutes() {
		if (this.routeMatcher.getRoutes() == null) {
			resetRoutes();
		}
		Map<String, String> values = new LinkedHashMap<>();
		for (Map.Entry<String, ZuulRoute> entry : this.routeMatcher.getRoutes().entrySet()) {
			values.put(entry.getKey(), entry.getValue().getLocation());
		}
		return values;
	}

	public ProxyRouteSpec getMatchingRoute(String path) {
		if (log.isDebugEnabled()) {
			log.debug("Finding route for path: " + path);
		}

		if (StringUtils.hasText(this.servletPath) && !this.servletPath.equals("/")
				&& path.startsWith(this.servletPath)) {
			path = path.substring(this.servletPath.length());
		}

		if(!matchesIgnoredPatterns(path)) {
			return toProxyRouteSpec(path, routeMatcher.getMatchingRoute(path));
		}
		return null;
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

	public void resetRoutes() {
		this.routeMatcher.setRoutes(locateRoutes());
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
			List<String> ignoredServices = this.properties.getIgnoredServices();
			String[] ignored = ignoredServices.toArray(new String[ignoredServices.size()]);
			for (String serviceId : services) {
				// Ignore specifically ignored services and those that were manually
				// configured
				String key = "/" + mapRouteToService(serviceId) + "/**";
				if (staticServices.containsKey(serviceId)
						&& staticServices.get(serviceId).getUrl() == null) {
					// Explicitly configured with no URL, cannot be ignored
					// all static routes are already in routesMap
					// Update location using serviceId if location is null
					ZuulRoute staticRoute = staticServices.get(serviceId);
					if (!StringUtils.hasText(staticRoute.getLocation())) {
						staticRoute.setLocation(serviceId);
					}
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

	protected String mapRouteToService(String serviceId) {
		return this.serviceRouteMapper.apply(serviceId);
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

	private ProxyRouteSpec toProxyRouteSpec(final String path, final ZuulProperties.ZuulRoute route) {
		if(route == null) {
			return null;
		}

		String targetPath = getRequestPath(path);
		String prefix = properties.getPrefix();
		final Boolean retryable = isRetryable(route);

		if (properties.isStripPrefix() && targetPath.startsWith(prefix)) {
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

		return new ProxyRouteSpec(
				route.getId(),
				targetPath,
				route.getLocation(),
				prefix,
				retryable
		);
	}

	private Boolean isRetryable(ZuulProperties.ZuulRoute route) {
		if (route.getRetryable() != null) {
			return route.getRetryable();
		}
		return properties.getRetryable();
	}

	private String getRequestPath(String path) {
		if (StringUtils.hasText(this.servletPath) && !this.servletPath.equals("/")
				&& path.startsWith(this.servletPath)) {
			return path.substring(this.servletPath.length());
		}
		return path;
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
