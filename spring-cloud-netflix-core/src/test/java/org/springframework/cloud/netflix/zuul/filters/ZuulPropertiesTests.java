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

import java.util.Arrays;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Dave Syer
 * @author Mathias Düsterhöft
 */
public class ZuulPropertiesTests {

	private ZuulProperties zuul;

	@Before
	public void setup() {
		this.zuul = new ZuulProperties();
	}

	@After
	public void teardown() {
		this.zuul = null;
	}

	@Test
	public void defaultIgnoredHeaders() {
		assertTrue(this.zuul.isIgnoreSecurityHeaders());
		assertTrue(this.zuul.getIgnoredHeaders()
				.containsAll(ZuulProperties.SECURITY_HEADERS));
	}

	@Test
	public void securityHeadersNotIgnored() {
		zuul.setIgnoreSecurityHeaders(false);

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
		assertTrue(this.zuul.getRoutes().get("foo").getSensitiveHeaders().isEmpty());
		assertTrue(this.zuul.getSensitiveHeaders()
				.containsAll(Arrays.asList("Cookie", "Set-Cookie", "Authorization")));
		assertFalse(route.isCustomSensitiveHeaders());
	}

	@Test
	public void addSensitiveHeaders() {
		this.zuul.setSensitiveHeaders(Collections.singleton("x-bar"));
		ZuulRoute route = new ZuulRoute("foo");
		route.setSensitiveHeaders(Collections.singleton("x-foo"));
		this.zuul.getRoutes().put("foo", route);
		ZuulRoute foo = this.zuul.getRoutes().get("foo");
		assertTrue(foo.getSensitiveHeaders().contains("x-foo"));
		assertFalse(foo.getSensitiveHeaders().contains("Cookie"));
		assertTrue(foo.isCustomSensitiveHeaders());
		assertTrue(this.zuul.getSensitiveHeaders().contains("x-bar"));
		assertFalse(this.zuul.getSensitiveHeaders().contains("Cookie"));
	}

	@Test
	public void createWithSensitiveHeaders() {
		this.zuul.setSensitiveHeaders(Collections.singleton("x-bar"));
		ZuulRoute route = new ZuulRoute("foo", "/path", "foo", "/path",
				false, false, Collections.singleton("x-foo"));
		this.zuul.getRoutes().put("foo", route);
		ZuulRoute foo = this.zuul.getRoutes().get("foo");
		assertTrue(foo.getSensitiveHeaders().contains("x-foo"));
		assertFalse(foo.getSensitiveHeaders().contains("Cookie"));
		assertTrue(foo.isCustomSensitiveHeaders());
		assertTrue(this.zuul.getSensitiveHeaders().contains("x-bar"));
		assertFalse(this.zuul.getSensitiveHeaders().contains("Cookie"));
	}

	@Test
	public void defaultHystrixThreadPool() {
		assertFalse(this.zuul.getThreadPool().isUseSeparateThreadPools());
		assertEquals("", this.zuul.getThreadPool().getThreadPoolKeyPrefix());
	}
}
