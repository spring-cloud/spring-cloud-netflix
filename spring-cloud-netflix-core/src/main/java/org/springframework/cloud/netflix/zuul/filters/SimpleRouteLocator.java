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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

/**
 * @author Dave Syer
 */
public class SimpleRouteLocator implements RouteLocator {

	private ZuulProperties properties;

	private PathMatcher pathMatcher = new AntPathMatcher();

	public SimpleRouteLocator(ZuulProperties properties) {
		this.properties = properties;
	}

	@Override
	public Map<String, String> getRoutes() {
		Map<String, String> paths = new LinkedHashMap<String, String>();
		for (ZuulRoute route : this.properties.getRoutes().values()) {
			paths.put(route.getPath(), route.getId());
		}
		return paths;
	}

	@Override
	public Collection<String> getIgnoredPaths() {
		return this.properties.getIgnoredPatterns();
	}

	@Override
	public Route getMatchingRoute(String path) {
		for (ZuulRoute route : this.properties.getRoutes().values()) {
			if (this.pathMatcher.match(route.getPath(), path)) {
				return route.getRoute(this.properties.getPrefix());
			}
		}
		return null;
	}
}
