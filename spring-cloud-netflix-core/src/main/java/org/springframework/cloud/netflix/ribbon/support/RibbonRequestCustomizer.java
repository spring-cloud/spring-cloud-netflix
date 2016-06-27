/*
 * Copyright 2013-2016 the original author or authors.
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
 *
 */

package org.springframework.cloud.netflix.ribbon.support;

import java.util.List;

/**
 * @author Spencer Gibb
 */
public interface RibbonRequestCustomizer<B> {
	boolean accepts(Class builderClass);
	void customize(B builder);

	class Runner {

		@SuppressWarnings("unchecked")
		public static void customize(List<RibbonRequestCustomizer> customizers, Object builder) {
			for (RibbonRequestCustomizer customizer : customizers) {
				if (customizer.accepts(builder.getClass())) {
					customizer.customize(builder);
				}
			}
		}
	}
}
