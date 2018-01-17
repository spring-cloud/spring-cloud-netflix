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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;

/**
 * @author Tom Cawley
 */
public class SimpleRouteLocatorTests {
	private ZuulProperties zuul = new ZuulProperties();

	public SimpleRouteLocatorTests() {
	}

	@Test
	public void test_getRoutesDefaultRouteAcceptor() {
		RouteLocator locator = new SimpleRouteLocator("/", this.zuul);
		this.zuul.getRoutes().clear();
		this.zuul.getRoutes().put("foo", new ZuulRoute("/foo/**", "foo"));

		assertThat(locator.getRoutes(), hasItem(createRoute("foo", "/**", "/foo")));
	}

	@Test
	public void test_getRoutesFilterRouteAcceptor() {
		RouteLocator locator = new FilteringRouteLocator("/", this.zuul);
		this.zuul.getRoutes().clear();
		this.zuul.getRoutes().put("foo", new ZuulRoute("/foo/**", "foo"));
		this.zuul.getRoutes().put("bar", new ZuulRoute("/bar/**", "bar"));

		final List<Route> routes = locator.getRoutes();
		assertThat(routes, hasItem(createRoute("bar", "/**", "/bar")));
		assertThat(routes, hasSize(1));
	}

	@Test
	public void testStripPrefix() {
		ZuulProperties properties = new ZuulProperties();
		properties.setPrefix("/test");
		properties.setStripPrefix(true);
		RouteLocator locator = new FilteringRouteLocator("/", properties);
		properties.getRoutes().put("testservicea", new ZuulRoute("/testservicea/**", "testservicea"));
		assertEquals("/test/testservicea/**", locator.getRoutes().get(0).getFullPath());
	}

	@Test
	public void testPrefix() {
		ZuulProperties properties = new ZuulProperties();
		properties.setPrefix("/test/");
		RouteLocator locator = new FilteringRouteLocator("/", properties);
		properties.getRoutes().put("testservicea", new ZuulRoute("/testservicea/**", "testservicea"));
		assertEquals("/test/testservicea/**", locator.getRoutes().get(0).getFullPath());
	}

	@Test
	public void test_getMatchingRouteFilterRouteAcceptor() {
		RouteLocator locator = new FilteringRouteLocator("/", this.zuul);
		this.zuul.getRoutes().clear();
		this.zuul.getRoutes().put("foo", new ZuulRoute("/foo/**", "foo"));
		this.zuul.getRoutes().put("bar", new ZuulRoute("/bar/**", "bar"));

		assertThat(locator.getMatchingRoute("/foo/1"), nullValue());
		assertThat(locator.getMatchingRoute("/bar/1"), is(createRoute("bar", "/1", "/bar")));
	}

	private Route createRoute(String id, String path, String prefix) {
		return new Route(id, path, id, prefix, false, null);
	}

	private static class FilteringRouteLocator extends SimpleRouteLocator {
		public FilteringRouteLocator(String servletPath, ZuulProperties properties) {
			super(servletPath, properties);
		}

		@Override
		public List<Route> getRoutes() {
			List<Route> values = new ArrayList<>();

			for (Entry<String, ZuulRoute> entry : getRoutesMap().entrySet()) {
				ZuulRoute route = entry.getValue();
				if (acceptRoute(route)) {
					String path = route.getPath();
					values.add(getRoute(route, path));
				}
			}
			return values;
		}

		private boolean acceptRoute(ZuulRoute route) {
			return route != null && !(route.getId().equals("foo"));
		}

		protected Route getRoute(ZuulRoute route, String path) {
			if (acceptRoute(route)) {
				return super.getRoute(route, path);
			}
			return null;
		}

		// For testing, expose as public so we can call getRoutesMap() directly.
		@Override
		public Map<String, ZuulRoute> getRoutesMap() {
			return super.getRoutesMap();
		}
	}
}