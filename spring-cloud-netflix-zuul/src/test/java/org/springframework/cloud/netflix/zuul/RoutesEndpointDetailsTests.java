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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Ryan Baxter
 * @author Gregor Zurowski
 */
@SpringBootTest
@RunWith(MockitoJUnitRunner.class)
public class RoutesEndpointDetailsTests {
	private RouteLocator locator;
	private RoutesEndpoint endpoint;
	@Mock
	private ApplicationEventPublisher publisher;

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
				routes.add(new Route("bar", "barpath", "barlocation", "bar-prefix", true, Collections.EMPTY_SET));
				return routes;
			}

			@Override
			public Route getMatchingRoute(String path) {
				return null;
			}
		};
		endpoint = spy(new RoutesEndpoint(locator));
	}

	@Test
	public void reset() throws Exception {
		this.endpoint.setApplicationEventPublisher(publisher);
		Map<String, String> result = new HashMap<>();
		for(Route r : locator.getRoutes()) {
			result.put(r.getFullPath(), r.getLocation());
		}
		assertEquals(result , endpoint.reset());
		verify(endpoint, times(1)).invoke();
		verify(publisher, times(1)).publishEvent(isA(RoutesRefreshedEvent.class));
	}

	@Test
	public void routeDetails() throws Exception {
		Map<String, RoutesEndpoint.RouteDetails> results = new HashMap<>();
		for (Route route : locator.getRoutes()) {
			results.put(route.getFullPath(), new RoutesEndpoint.RouteDetails(route));
		}
		assertEquals(results, this.endpoint.invokeRouteDetails(RoutesEndpoint.FORMAT_DETAILS));
		verify(endpoint, times(1)).invokeRouteDetails();
	}

}