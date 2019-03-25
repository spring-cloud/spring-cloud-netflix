/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Johannes Edmeier
 */
public class CompositeRouteLocatorTests {

	private CompositeRouteLocator locator;

	public CompositeRouteLocatorTests() {
		List<RouteLocator> locators = new ArrayList<>();
		locators.add(
				new TestRouteLocator(asList("ign1"), asList(createRoute("1", "/pathA"))));
		locators.add(new TestRouteLocator(asList("ign1", "ign2"),
				asList(createRoute("2", "/pathA"), createRoute("2", "/pathB"))));
		this.locator = new CompositeRouteLocator(locators);
	}

	@Test
	public void test_getIgnoredPaths() {
		assertThat(locator.getIgnoredPaths()).contains("ign1", "ign2");

	}

	@Test
	public void test_getRoutes() {
		assertThat(locator.getRoutes()).contains(createRoute("1", "/pathA"),
				createRoute("2", "/pathB"));
	}

	@Test
	public void test_getMatchingRoute() {
		assertThat(locator.getMatchingRoute("/pathA")).isNotNull();
		assertThat(locator.getMatchingRoute("/pathA").getId()).isEqualTo("1");
		assertThat(locator.getMatchingRoute("/pathB").getId())
				.as("Locator 1 should take precedence").isEqualTo("2");
		assertThat(locator.getMatchingRoute("/pathNot")).isNull();
	}

	@Test
	public void test_refresh() {
		RefreshableRouteLocator mock = mock(RefreshableRouteLocator.class);
		new CompositeRouteLocator(asList(mock)).refresh();
		verify(mock).refresh();
	}

	private Route createRoute(String id, String path) {
		return new Route(id, path, null, null, false, Collections.<String>emptySet());
	}

	private static class TestRouteLocator implements RouteLocator {

		private Collection<String> ignoredPaths;

		private List<Route> routes;

		TestRouteLocator(Collection<String> ignoredPaths, List<Route> routes) {
			this.ignoredPaths = ignoredPaths;
			this.routes = routes;
		}

		@Override
		public Collection<String> getIgnoredPaths() {
			return this.ignoredPaths;
		}

		@Override
		public List<Route> getRoutes() {
			return this.routes;
		}

		@Override
		public Route getMatchingRoute(String path) {
			for (Route route : routes) {
				if (path.startsWith(route.getPath())) {
					return route;
				}
			}
			return null;
		}

	}

}
