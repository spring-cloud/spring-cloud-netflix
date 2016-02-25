/*
 * Copyright 2013-2015 the original author or authors.
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

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.mock.web.MockHttpServletRequest;

import com.netflix.zuul.context.RequestContext;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Dave Syer
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
		assertNotNull(this.mapping.getHandler(this.request));
	}

	@Test
	public void defaultPath() throws Exception {
		Mockito.when(this.locator.getRoutes()).thenReturn(Collections
				.singletonList(new Route("default", "/**", "foo", "", null, null)));
		;
		this.request.setServletPath("/");
		this.mapping.setDirty(true);
		assertNotNull(this.mapping.getHandler(this.request));
	}

	@Test
	public void errorPath() throws Exception {
		Mockito.when(this.locator.getRoutes()).thenReturn(Collections
				.singletonList(new Route("default", "/**", "foo", "", null, null)));
		this.request.setServletPath("/error");
		this.mapping.setDirty(true);
		assertNull(this.mapping.getHandler(this.request));
	}

}
