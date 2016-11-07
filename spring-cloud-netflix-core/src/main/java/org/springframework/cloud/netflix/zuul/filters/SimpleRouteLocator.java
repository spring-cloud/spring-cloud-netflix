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

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

/**
 * Simple {@link RouteLocator} based on configuration data held in {@link ZuulProperties}.
 *
 * @author Dave Syer
 * @author Johannes Edmeier
 */
public class SimpleRouteLocator extends AbstractZuulRouteLocator implements RouteLocator, Ordered {
	private static final int DEFAULT_ORDER = 0;
	private int order = DEFAULT_ORDER;

	private ZuulProperties properties;

	public SimpleRouteLocator(String servletPath, ZuulProperties properties) {
		super(servletPath, properties);
		this.properties = properties;
	}

	@Override
	protected Map<String, ZuulRoute> locateRoutes() {
		LinkedHashMap<String, ZuulRoute> routesMap = new LinkedHashMap<>();
		for (ZuulRoute route : this.properties.getRoutes().values()) {
			if (StringUtils.hasText(route.getUrl())) {
				String path = addPrefix(route.getPath());
				routesMap.put(path, route);
			}
		}
		return routesMap;
	}

	private String addPrefix(String path) {
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		if (StringUtils.hasText(this.properties.getPrefix())) {
			path = this.properties.getPrefix() + path;
			if (!path.startsWith("/")) {
				path = "/" + path;
			}
		}
		return path;
	}

	@Override
	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

}
