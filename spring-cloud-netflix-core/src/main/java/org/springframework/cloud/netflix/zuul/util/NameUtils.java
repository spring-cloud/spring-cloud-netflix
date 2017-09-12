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
 *
 */

package org.springframework.cloud.netflix.zuul.util;

import org.springframework.cloud.netflix.zuul.route.predicate.RoutePredicateFactory;

/**
 * @author Spencer Gibb
 * @author Mustansar Anwar
 */
public class NameUtils {
	public static final String GENERATED_NAME_PREFIX = "_genkey_";

	public static String generateName(int i) {
		return GENERATED_NAME_PREFIX + i;
	}

	public static String normalizePredicateName(Class<? extends RoutePredicateFactory> clazz) {
		return clazz.getSimpleName().replace(RoutePredicateFactory.class.getSimpleName(), "");
	}
}
