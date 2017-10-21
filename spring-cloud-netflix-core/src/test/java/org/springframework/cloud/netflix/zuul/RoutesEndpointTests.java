/*
 *
 *  * Copyright 2013-2016 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.cloud.netflix.zuul;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.netflix.zuul.RoutesEndpoint;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Ryan Baxter
 * @author Gregor Zurowski
 */
public class RoutesEndpointTests {

	private RouteLocator locator;

	@Before
	public void setUp() {
		this.locator = new RouteLocator() {
			@Override
			public Collection<String> getIgnoredPaths() {
				return null;
			}

			@Override
			public List<Route> getRoutes() {
				List<Route> routes = new ArrayList<>();
				routes.add(new Route("foo", "foopath", "foolocation", null, true, Collections.EMPTY_SET));
				routes.add(new Route("bar", "barpath", "barlocation", "/bar-prefix", true, Collections.EMPTY_SET));
				return routes;
			}

			@Override
			public Route getMatchingRoute(String path) {
				return null;
			}
		};
	}

	@Test
	public void testInvoke() {
		RoutesEndpoint endpoint = new RoutesEndpoint(locator);
		Map<String, String> result = new HashMap<String, String>();
		for(Route r : locator.getRoutes()) {
			result.put(r.getFullPath(), r.getLocation());
		}
		assertEquals(result , endpoint.invoke());
	}

	@Test
	public void testInvokeRouteDetails() {
		RoutesEndpoint endpoint = new RoutesEndpoint(locator);
		Map<String, RoutesEndpoint.RouteDetails> results = new HashMap<>();
		for (Route route : locator.getRoutes()) {
			results.put(route.getFullPath(), new RoutesEndpoint.RouteDetails(route));
		}
		assertEquals(results, endpoint.invokeRouteDetails());
	}

	@Test
	public void testId() {
		RoutesEndpoint endpoint = new RoutesEndpoint(locator);
		assertEquals("routes", endpoint.getId());
	}

	@Test
	public void testIsSensitive() {
		RoutesEndpoint endpoint = new RoutesEndpoint(locator);
		assertTrue(endpoint.isSensitive());
	}
}