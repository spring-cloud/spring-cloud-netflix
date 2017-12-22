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

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;

import com.google.common.collect.ImmutableMap;
import org.springframework.http.HttpMethod;

/**
 * @author Tom Cawley
 * @author Arnold Galovics
 */
@RunWith(MockitoJUnitRunner.class)
public class SimpleRouteLocatorTests {
	@Mock
	private ZuulProperties zuulProperties;

	private SimpleRouteLocator underTest;

	@Before
	public void setUp() {
		given(zuulProperties.getPrefix()).willReturn("");
		given(zuulProperties.isStripPrefix()).willReturn(true);
		underTest = new SimpleRouteLocator("/", zuulProperties);
	}

	@Test
	public void testGetRoutesWorkProperly() {
		// given
		given(zuulProperties.getRoutes()).willReturn(ImmutableMap.of("foo", new ZuulRoute("/foo/**", "foo")));
		// when
		List<Route> result = underTest.getRoutes();
		// then
		assertThat(result).hasSize(1).containsExactly(createRoute("foo", "/**", "/foo"));
	}

	@Test
	public void testGetMatchingRouteShouldReturnRouteWhenMatches() {
		// given
		given(zuulProperties.getRoutes()).willReturn(ImmutableMap.of("foo", createZuulRoute("/foo/**", "foo", HttpMethod.GET)));
		// when
		Route result = underTest.getMatchingRoute(RequestWrapper.from("/foo/1", HttpMethod.GET));
		// then
		assertThat(result).isNotNull().isEqualTo(createRoute("foo", "/1", "/foo"));
	}

	@Test
	public void testGetMatchingRouteShouldReturnRouteWhenNullMethodGiven() {
		// given
		given(zuulProperties.getRoutes()).willReturn(ImmutableMap.of("foo", createZuulRoute("/foo/**", "foo", HttpMethod.GET)));
		// when
		Route result = underTest.getMatchingRoute(RequestWrapper.fromPath("/foo/1"));
		// then
		assertThat(result).isNotNull().isEqualTo(createRoute("foo", "/1", "/foo"));
	}

    @Test
    public void testGetMatchingRouteShouldReturnRouteWhenNullMethodGivenInProperties() {
        // given
        given(zuulProperties.getRoutes()).willReturn(ImmutableMap.of("foo", new ZuulRoute("/foo/**", "foo")));
        // when
        Route result = underTest.getMatchingRoute(RequestWrapper.from("/foo/1", HttpMethod.GET));
        // then
        assertThat(result).isNotNull().isEqualTo(createRoute("foo", "/1", "/foo"));
    }

	@Test
	public void testGetMatchingRouteShouldReturnNullWhenMethodDoesNotMatch() {
		// given
		given(zuulProperties.getRoutes()).willReturn(ImmutableMap.of("foo", createZuulRoute("/foo/**", "foo", HttpMethod.GET)));
		// when
		Route result = underTest.getMatchingRoute(RequestWrapper.from("/foo/1", HttpMethod.POST));
		// then
		assertThat(result).isNull();
	}

	private ZuulRoute createZuulRoute(String path, String location, HttpMethod method) {
		ZuulRoute zuulRoute = new ZuulRoute(path, location);
		zuulRoute.setMethod(method);
		return zuulRoute;
	}

	private Route createRoute(String id, String path, String prefix) {
		return new Route(id, path, id, prefix, false, null);
	}
}