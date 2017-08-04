/*
 * Copyright 2013-2017 the original author or authors.
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

package org.springframework.cloud.netflix.zuul.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.mock.web.MockHttpServletRequest;

import com.netflix.zuul.context.RequestContext;

/**
 * @author Dave Syer
 * @author Biju Kunjummen
 */
public class ZuulHandlerMappingTests {

	private ZuulHandlerMapping mapping;

	private RouteLocator locator = Mockito.mock(RouteLocator.class);

	private ErrorController errors = Mockito.mock(ErrorController.class);

	private MockHttpServletRequest request = new MockHttpServletRequest();

	@Before
	public void init() {
		RequestContext.getCurrentContext().clear();
		this.mapping = new ZuulHandlerMapping(this.locator, new ZuulController());
		this.mapping.setErrorController(this.errors);
		Mockito.when(this.errors.getErrorPath()).thenReturn("/error");
	}

	@Test
	public void mappedPath() throws Exception {
		Mockito.when(this.locator.getRoutes()).thenReturn(Collections
				.singletonList(new Route("foo", "/foo/**", "foo", "", null, null)));
		this.request.setServletPath("/foo/");
		this.mapping.setDirty(true);
		assertThat(this.mapping.getHandler(this.request)).isNotNull();
	}

	@Test
	public void defaultPath() throws Exception {
		Mockito.when(this.locator.getRoutes()).thenReturn(Collections
				.singletonList(new Route("default", "/**", "foo", "", null, null)));
		;
		this.request.setServletPath("/");
		this.mapping.setDirty(true);
		assertThat(this.mapping.getHandler(this.request)).isNotNull();
	}

	@Test
	public void errorPath() throws Exception {
		Mockito.when(this.locator.getRoutes()).thenReturn(Collections
				.singletonList(new Route("default", "/**", "foo", "", null, null)));
		this.request.setServletPath("/error");
		this.mapping.setDirty(true);
		assertThat(this.mapping.getHandler(this.request)).isNull();
	}

	@Test
	public void ignoredPathsShouldNotReturnAHandler() throws Exception {
		assertThat(mappingWithIgnoredPathsAndRoutes(Arrays.asList("/p1/**"),
				new Route("p1", "/p1/**", "p1", "", null, null))
						.getHandler(requestForAPath("/p1"))).isNull();

		assertThat(mappingWithIgnoredPathsAndRoutes(Arrays.asList("/p1/**/p3/"),
				new Route("p1", "/p1/**/p3", "p1", "", null, null))
				.getHandler(requestForAPath("/p1/p2/p3"))).isNull();

		assertThat(mappingWithIgnoredPathsAndRoutes(Arrays.asList("/p1/**/p3/**"),
				new Route("p1", "/p1/**/p3", "p1", "", null, null))
				.getHandler(requestForAPath("/p1/p2/p3"))).isNull();

		assertThat(mappingWithIgnoredPathsAndRoutes(Arrays.asList("/p1/**/p4/"),
				new Route("p1", "/p1/**/p4/", "p1", "", null, null))
				.getHandler(requestForAPath("/p1/p2/p3/p4"))).isNull();
	}
	
	private ZuulHandlerMapping mappingWithIgnoredPathsAndRoutes(List<String> ignoredPaths, Route route) {
		RouteLocator routeLocator = Mockito.mock(RouteLocator.class);
		Mockito.when(routeLocator.getIgnoredPaths())
				.thenReturn(ignoredPaths);
		Mockito.when(routeLocator.getRoutes()).thenReturn(Collections.singletonList(route));
		ZuulHandlerMapping zuulHandlerMapping = new ZuulHandlerMapping(routeLocator, new ZuulController());
		return zuulHandlerMapping;
	}
	
	private MockHttpServletRequest requestForAPath(String path) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setServletPath(path);
		return request;
	}

}
