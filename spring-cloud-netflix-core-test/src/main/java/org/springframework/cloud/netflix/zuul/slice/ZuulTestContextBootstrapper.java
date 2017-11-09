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

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.TestContextBootstrapper;

/**
 * {@link TestContextBootstrapper} for {@link ZuulProxyTest @ZuulProxyTest} support.
 * 
 * This loads up a Mock Web Environment to test Zuul configuration using
 * {@link org.springframework.test.web.servlet.MockMvc}
 *
 * @author Biju Kunjummen
 */
class ZuulTestContextBootstrapper extends SpringBootTestContextBootstrapper {
	@Override
	protected SpringBootTest.WebEnvironment getWebEnvironment(Class<?> testClass) {
		return SpringBootTest.WebEnvironment.MOCK;
	}

	@Override
	protected String[] getProperties(Class<?> testClass) {
		return AnnotatedElementUtils.getMergedAnnotation(testClass, ZuulProxyTest.class)
				.properties();
	}
}
