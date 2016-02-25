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

package org.springframework.cloud.netflix.zuul.filters;

import java.util.Collections;

import org.junit.Test;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Dave Syer
 */
public class ZuulPropertiesTests {

	private ZuulProperties zuul = new ZuulProperties();

	@Test
	public void defaultIgnoredHeaders() {
		assertTrue(this.zuul.getIgnoredHeaders().isEmpty());
	}

	@Test
	public void addIgnoredHeaders() {
		this.zuul.setIgnoredHeaders(Collections.singleton("x-foo"));
		assertTrue(this.zuul.getIgnoredHeaders().contains("x-foo"));
	}

	@Test
	public void defaultSensitiveHeaders() {
		ZuulRoute route = new ZuulRoute("foo");
		this.zuul.getRoutes().put("foo", route);
		assertTrue(this.zuul.getRoutes().get("foo").getSensitiveHeaders()
				.contains("Cookie"));
	}

	@Test
	public void addSensitiveHeaders() {
		ZuulRoute route = new ZuulRoute("foo");
		route.setSensitiveHeaders(Collections.singleton("x-foo"));
		this.zuul.getRoutes().put("foo", route);
		assertFalse(this.zuul.getRoutes().get("foo").getSensitiveHeaders()
				.contains("Cookie"));
	}

}
