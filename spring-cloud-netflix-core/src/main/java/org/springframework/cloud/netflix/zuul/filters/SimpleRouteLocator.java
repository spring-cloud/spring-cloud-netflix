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
import java.util.LinkedHashSet;

/**
 * @author Dave Syer
 */
public class SimpleRouteLocator implements RouteLocator {

	private final ZuulProperties properties;

	private final ZuulRouteStore routeStore;

	public SimpleRouteLocator(ZuulProperties properties, ZuulRouteStore routeStore) {
		this.properties = properties;
		this.routeStore = routeStore;
	}

	@Override
	public Collection<String> getRoutePaths() {
		Collection<String> paths = new LinkedHashSet<String>();
		for (ZuulRoute route : routeStore.getRoutes().values()) {
			paths.add(route.getPath());
		}
		return paths;
	}

	@Override
	public Collection<String> getIgnoredPaths() {
		return this.properties.getIgnoredPatterns();
	}

}
