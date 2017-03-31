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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.springframework.util.ReflectionUtils;

import com.netflix.zuul.FilterLoader;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.filters.FilterRegistry;
import com.netflix.zuul.monitoring.CounterFactory;
import com.netflix.zuul.monitoring.TracerFactory;

import lombok.extern.apachecommons.CommonsLog;

/**
 * @author Spencer Gibb
 *
 * TODO: .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
 */
@CommonsLog
public class ZuulFilterInitializer implements ServletContextListener {

	private final Map<String, ZuulFilter> filters;
	private final CounterFactory counterFactory;
	private final TracerFactory tracerFactory;

	public ZuulFilterInitializer(Map<String, ZuulFilter> filters,
								 CounterFactory counterFactory,
								 TracerFactory tracerFactory) {
		this.filters = filters;
		this.counterFactory = counterFactory;
		this.tracerFactory = tracerFactory;
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {

		log.info("Starting filter initializer context listener");

		TracerFactory.initialize(tracerFactory);
		CounterFactory.initialize(counterFactory);

		FilterRegistry registry = FilterRegistry.instance();

		for (Map.Entry<String, ZuulFilter> entry : this.filters.entrySet()) {
			registry.put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		log.info("Stopping filter initializer context listener");
		FilterRegistry registry = FilterRegistry.instance();
		for (Map.Entry<String, ZuulFilter> entry : this.filters.entrySet()) {
			registry.remove(entry.getKey());
		}
		clearLoaderCache();

		TracerFactory.initialize(null);
		CounterFactory.initialize(null);
	}

	private void clearLoaderCache() {
		FilterLoader instance = FilterLoader.getInstance();
		Field field = ReflectionUtils.findField(FilterLoader.class, "hashFiltersByType");
		ReflectionUtils.makeAccessible(field);
		@SuppressWarnings("rawtypes")
		Map cache = (Map) ReflectionUtils.getField(field, instance);
		cache.clear();
	}

	/*
	 * private void initGroovyFilterManager() {
	 * 
	 * //TODO: support groovy filters loaded from filesystem in proxy
	 * FilterLoader.getInstance().setCompiler(new GroovyCompiler());
	 * 
	 * final String scriptRoot = props.getFilterRoot();
	 * log.info("Using file system script: " + scriptRoot);
	 * 
	 * try { FilterFileManager.setFilenameFilter(new GroovyFileFilter());
	 * FilterFileManager.init(5, scriptRoot + "/pre", scriptRoot + "/route", scriptRoot +
	 * "/post" ); } catch (Exception e) { throw new RuntimeException(e); } }
	 */
}
