/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.netflix.zuul.slice;

import com.netflix.zuul.ZuulFilter;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.autoconfigure.filter.AnnotationCustomizableTypeExcludeFilter;
import org.springframework.cloud.netflix.zuul.filters.discovery.ServiceRouteMapper;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * {@link TypeExcludeFilter} that scans and loads bean that are relevant to {@link ZuulProxyTest}
 * 
 * @author Biju Kunjummen
 */
public class ZuulTypeExcludeFilter extends AnnotationCustomizableTypeExcludeFilter {
	private static final Set<Class<?>> DEFAULT_INCLUDES;

	static {
		Set<Class<?>> includes = new LinkedHashSet<>();
		includes.add(ZuulFilter.class);
		includes.add(ServiceRouteMapper.class);
		
		DEFAULT_INCLUDES = Collections.unmodifiableSet(includes);
	};

	private final ZuulProxyTest annotation;

	ZuulTypeExcludeFilter(Class<?> testClass) {
		this.annotation = AnnotatedElementUtils.getMergedAnnotation(testClass,
				ZuulProxyTest.class);
	}

	@Override
	protected boolean hasAnnotation() {
		return this.annotation != null;
	}

	@Override
	protected ComponentScan.Filter[] getFilters(FilterType type) {
		return new ComponentScan.Filter[0];
	}

	@Override
	protected boolean isUseDefaultFilters() {
		return true;
	}

	@Override
	protected Set<Class<?>> getDefaultIncludes() {
		return DEFAULT_INCLUDES;
	}

	@Override
	protected Set<Class<?>> getComponentIncludes() {
		return Collections.emptySet();
	}
}
