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
import java.util.Map.Entry;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.filters.AbstractZuulRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.RefreshableRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;
import org.springframework.core.Ordered;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

import lombok.extern.apachecommons.CommonsLog;

/**
 * A {@link RouteLocator} that combines static, configured routes with those from a
 * {@link DiscoveryClient}. The discovery client takes precedence.
 *
 * @author Spencer Gibb
 * @author Dave Syer
 * @author Johannes Edmeier
 */
@CommonsLog
public class DiscoveryClientRouteLocator extends AbstractZuulRouteLocator
		implements RefreshableRouteLocator, Ordered {
	public static final String DEFAULT_ROUTE = "/**";
	private static final int DEFAULT_ORDER = 100;
	private DiscoveryClient discovery;
	private ZuulProperties properties;
	private ServiceRouteMapper serviceRouteMapper;
	private int order = DEFAULT_ORDER;

	public DiscoveryClientRouteLocator(String servletPath, DiscoveryClient discovery,
			ZuulProperties properties) {
		super(servletPath, properties);

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
	}

	public DiscoveryClientRouteLocator(String servletPath, DiscoveryClient discovery,
			ZuulProperties properties, ServiceRouteMapper serviceRouteMapper) {
		this(servletPath, discovery, properties);
		this.serviceRouteMapper = serviceRouteMapper;
	}

	@Override
	protected LinkedHashMap<String, ZuulRoute> locateRoutes() {
		LinkedHashMap<String, ZuulRoute> routesMap = new LinkedHashMap<>();
		addStaticServices(routesMap);
		if (this.discovery != null) {
			addDiscoveryServices(routesMap);
		}
		ZuulRoute defaultRoute = this.properties.getRoutes().get(DEFAULT_ROUTE);
		if (defaultRoute != null) {
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

	private void addDiscoveryServices(LinkedHashMap<String, ZuulRoute> routesMap) {
		// Add routes for discovery services by default
		List<String> services = this.discovery.getServices();
		String[] ignored = this.properties.getIgnoredServices()
				.toArray(new String[this.properties.getIgnoredServices().size()]);
		for (String serviceId : services) {
			// Ignore specifically ignored services and those that were manually
			// configured
			if (!PatternMatchUtils.simpleMatch(ignored, serviceId)) {
				// Not ignored
				String key = "/" + mapRouteToService(serviceId) + "/**";
				routesMap.put(key, new ZuulRoute(key, serviceId));
			}
		}
	}

	private void addStaticServices(LinkedHashMap<String, ZuulRoute> routesMap) {
		for (ZuulRoute route : this.properties.getRoutes().values()) {
			String serviceId = route.getServiceId();
			if (serviceId == null) {
				serviceId = route.getId();
			}
			if (serviceId != null && route.getUrl() == null) {
				if (!StringUtils.hasText(route.getLocation())) {
					// Update location using serviceId if location is null
					route.setLocation(serviceId);
				}
				routesMap.put(route.getPath(), route);
			}
		}
	}

	@Override
	public void refresh() {
		doRefresh();
	}

	protected String mapRouteToService(String serviceId) {
		return this.serviceRouteMapper.apply(serviceId);
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}
}
