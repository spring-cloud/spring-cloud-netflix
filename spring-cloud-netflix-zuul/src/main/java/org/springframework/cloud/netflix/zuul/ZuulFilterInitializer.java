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

package org.springframework.cloud.netflix.zuul;

import java.lang.reflect.Field;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.ReflectionUtils;

import com.netflix.zuul.FilterLoader;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.filters.FilterRegistry;
import com.netflix.zuul.monitoring.CounterFactory;
import com.netflix.zuul.monitoring.TracerFactory;

/**
 * Initializes various Zuul components including {@link ZuulFilter}.
 *
 * @author Spencer Gibb
 *
 */
public class ZuulFilterInitializer {

	private static final Log log = LogFactory.getLog(ZuulFilterInitializer.class);

	private final Map<String, ZuulFilter> filters;
	private final CounterFactory counterFactory;
	private final TracerFactory tracerFactory;
	private final FilterLoader filterLoader;
	private final FilterRegistry filterRegistry;

	public ZuulFilterInitializer(Map<String, ZuulFilter> filters,
								 CounterFactory counterFactory,
								 TracerFactory tracerFactory,
								 FilterLoader filterLoader,
								 FilterRegistry filterRegistry) {
		this.filters = filters;
		this.counterFactory = counterFactory;
		this.tracerFactory = tracerFactory;
		this.filterLoader = filterLoader;
		this.filterRegistry = filterRegistry;
	}

	@PostConstruct
	public void contextInitialized() {
		log.info("Starting filter initializer");

		TracerFactory.initialize(tracerFactory);
		CounterFactory.initialize(counterFactory);

		for (Map.Entry<String, ZuulFilter> entry : this.filters.entrySet()) {
			filterRegistry.put(entry.getKey(), entry.getValue());
		}
	}

	@PreDestroy
	public void contextDestroyed() {
		log.info("Stopping filter initializer");
		for (Map.Entry<String, ZuulFilter> entry : this.filters.entrySet()) {
			filterRegistry.remove(entry.getKey());
		}
		clearLoaderCache();

		TracerFactory.initialize(null);
		CounterFactory.initialize(null);
	}

	private void clearLoaderCache() {
		Field field = ReflectionUtils.findField(FilterLoader.class, "hashFiltersByType");
		ReflectionUtils.makeAccessible(field);
		@SuppressWarnings("rawtypes")
		Map cache = (Map) ReflectionUtils.getField(field, filterLoader);
		cache.clear();
	}

}
