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
import java.util.List;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;

/**
 * RouteLocator that composes multiple RouteLocators.
 *
 * @author Johannes Edmeier
 *
 */
public class CompositeRouteLocator implements RefreshableRouteLocator {
	private final Collection<? extends RouteLocator> routeLocators;
	private ArrayList<RouteLocator> rl;

	public CompositeRouteLocator(Collection<? extends RouteLocator> routeLocators) {
		Assert.notNull(routeLocators, "'routeLocators' must not be null");
		rl = new ArrayList<>(routeLocators);
		AnnotationAwareOrderComparator.sort(rl);
		this.routeLocators = rl;
	}

	@Override
	public Collection<String> getIgnoredPaths() {
		List<String> ignoredPaths = new ArrayList<>();
		for (RouteLocator locator : routeLocators) {
			ignoredPaths.addAll(locator.getIgnoredPaths());
		}
		return ignoredPaths;
	}

	@Override
	public List<Route> getRoutes() {
		List<Route> route = new ArrayList<>();
		for (RouteLocator locator : routeLocators) {
			route.addAll(locator.getRoutes());
		}
		return route;
	}

	@Override
	public Route getMatchingRoute(String path) {
		for (RouteLocator locator : routeLocators) {
			Route route = locator.getMatchingRoute(path);
			if (route != null) {
				return route;
			}
		}
		return null;
	}

	@Override
	public void refresh() {
		for (RouteLocator locator : routeLocators) {
			if (locator instanceof RefreshableRouteLocator) {
				((RefreshableRouteLocator) locator).refresh();
			}
		}
	}
}