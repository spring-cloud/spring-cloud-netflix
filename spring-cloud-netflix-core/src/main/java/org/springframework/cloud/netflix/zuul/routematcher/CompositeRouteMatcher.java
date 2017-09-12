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
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;

/**
 * RouteMatcher that composes multiple RouteMatchers.
 * 
 * This is mainly copied from {@link CompositeRouteLocator}
 *
 * @author Johannes Edmeier
 * @author Mustansar Anwar
 *
 */
public class CompositeRouteMatcher implements RefreshableRouteMatcher {
	private final Collection<? extends RouteMatcher> routeMatchers;
	private ArrayList<RouteMatcher> rl;

	public CompositeRouteMatcher(Collection<? extends RouteMatcher> routeMatchers) {
		Assert.notNull(routeMatchers, "'routeMatchers' must not be null");
		rl = new ArrayList<>(routeMatchers);
		AnnotationAwareOrderComparator.sort(rl);
		this.routeMatchers = rl;
	}

	@Override
	public Collection<String> getIgnoredPaths() {
		List<String> ignoredPaths = new ArrayList<>();
		for (RouteMatcher matcher : routeMatchers) {
			ignoredPaths.addAll(matcher.getIgnoredPaths());
		}
		return ignoredPaths;
	}

	@Override
	public List<Route> getRoutes() {
		List<Route> route = new ArrayList<>();
		for (RouteMatcher matcher : routeMatchers) {
			route.addAll(matcher.getRoutes());
		}
		return route;
	}

	@Override
	public Route getMatchingRoute(HttpServletRequest request) {
		for (RouteMatcher matcher : routeMatchers) {
			Route route = matcher.getMatchingRoute(request);
			if (route != null) {
				return route;
			}
		}
		return null;
	}

	@Override
	public void refresh() {
		for (RouteMatcher matcher : routeMatchers) {
			if (matcher instanceof RefreshableRouteMatcher) {
				((RefreshableRouteMatcher) matcher).refresh();
			}
		}
	}

	@Override
	public Route getMatchingRoute(String path) {
		throw new RuntimeException("Encountered unreachable code flow.");
	}
}