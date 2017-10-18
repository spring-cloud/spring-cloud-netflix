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

package org.springframework.cloud.netflix.zuul.filters.discovery;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.filters.RefreshableRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.SimpleRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * A {@link RouteLocator} that combines static, configured routes with those from a
 * {@link DiscoveryClient}. The discovery client takes precedence.
 *
 * @author Spencer Gibb
 * @author Dave Syer
 */
public class DiscoveryClientRouteLocator extends SimpleRouteLocator
		implements RefreshableRouteLocator {

	private static final Log log = LogFactory.getLog(DiscoveryClientRouteLocator.class);

	public static final String DEFAULT_ROUTE = "/**";

	private DiscoveryClient discovery;

	private ZuulProperties properties;

	private ServiceRouteMapper serviceRouteMapper;

	@Deprecated
	public DiscoveryClientRouteLocator(String servletPath, DiscoveryClient discovery,
									   ZuulProperties properties) {
		this(servletPath, discovery, properties, (ServiceInstance)null);
	}

	public DiscoveryClientRouteLocator(String servletPath, DiscoveryClient discovery,
			ZuulProperties properties, ServiceInstance localServiceInstance) {
		super(servletPath, properties);

		if (properties.isIgnoreLocalService() && localServiceInstance != null) {
			String localServiceId = localServiceInstance.getServiceId();
			if (!properties.getIgnoredServices().contains(localServiceId)) {
				properties.getIgnoredServices().add(localServiceId);
			}
		}
		this.serviceRouteMapper = new SimpleServiceRouteMapper();
		this.discovery = discovery;
		this.properties = properties;
	}

	@Deprecated
	public DiscoveryClientRouteLocator(String servletPath, DiscoveryClient discovery,
			ZuulProperties properties, ServiceRouteMapper serviceRouteMapper) {
		this(servletPath, discovery, properties, (ServiceInstance)null);
		this.serviceRouteMapper = serviceRouteMapper;
	}

	public DiscoveryClientRouteLocator(String servletPath, DiscoveryClient discovery,
			ZuulProperties properties, ServiceRouteMapper serviceRouteMapper, ServiceInstance localServiceInstance) {
		this(servletPath, discovery, properties, localServiceInstance);
		this.serviceRouteMapper = serviceRouteMapper;
	}

	public void addRoute(String path, String location) {
		this.properties.getRoutes().put(path, new ZuulRoute(path, location));
		refresh();
	}

	public void addRoute(ZuulRoute route) {
		this.properties.getRoutes().put(route.getPath(), route);
		refresh();
	}

	@Override
	protected LinkedHashMap<String, ZuulRoute> locateRoutes() {
		LinkedHashMap<String, ZuulRoute> routesMap = new LinkedHashMap<String, ZuulRoute>();
		routesMap.putAll(super.locateRoutes());
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

	@Override
	public void refresh() {
		doRefresh();
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

}
