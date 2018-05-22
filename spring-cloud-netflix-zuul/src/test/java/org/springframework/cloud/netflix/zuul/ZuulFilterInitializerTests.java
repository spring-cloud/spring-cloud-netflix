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

package org.springframework.cloud.netflix.zuul;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

import com.netflix.zuul.FilterLoader;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.filters.FilterRegistry;
import com.netflix.zuul.monitoring.CounterFactory;
import com.netflix.zuul.monitoring.TracerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class ZuulFilterInitializerTests {

	private Map<String, ZuulFilter> filters = getFilters();
	private CounterFactory counterFactory = mock(CounterFactory.class);
	private TracerFactory tracerFactory = mock(TracerFactory.class);
	private FilterLoader filterLoader = new FilterLoader();
	private FilterRegistry filterRegistry = getFilterRegistry();

	private final ZuulFilterInitializer initializer = new ZuulFilterInitializer(filters,
			counterFactory, tracerFactory, filterLoader, filterRegistry);

	@Test
	public void shouldSetupOnContextInitializedEvent() throws Exception {
		initializer.contextInitialized();

		assertEquals(tracerFactory, TracerFactory.instance());
		assertEquals(counterFactory, CounterFactory.instance());
		assertThat(filterRegistry.getAllFilters())
				.containsAll(filters.values());
	}

	@Test
	public void shouldCleanupOnContextDestroyed() throws Exception {
		initializer.contextDestroyed();

		assertEquals(null, ReflectionTestUtils.getField(TracerFactory.class, "INSTANCE"));
		assertEquals(null,
				ReflectionTestUtils.getField(CounterFactory.class, "INSTANCE"));
		assertTrue(FilterRegistry.instance().getAllFilters().isEmpty());
		assertTrue(getHashFiltersByType().isEmpty());
	}

	private Map getHashFiltersByType() {
		Field field = ReflectionUtils.findField(FilterLoader.class, "hashFiltersByType");
		ReflectionUtils.makeAccessible(field);
		return (Map) ReflectionUtils.getField(field, FilterLoader.getInstance());
	}

	private Map<String, ZuulFilter> getFilters() {
		Map<String, ZuulFilter> filters = new HashMap<>();
		filters.put("key1", mock(ZuulFilter.class));
		filters.put("key2", mock(ZuulFilter.class));
		return filters;
	}

	private FilterRegistry getFilterRegistry() {
		try {
			Constructor<FilterRegistry> constructor = FilterRegistry.class
					.getDeclaredConstructor(new Class[0]);
			constructor.setAccessible(true);
			return constructor.newInstance(new Object[0]);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}