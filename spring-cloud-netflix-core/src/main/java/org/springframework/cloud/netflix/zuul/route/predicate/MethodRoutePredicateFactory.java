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

package org.springframework.cloud.netflix.zuul.route.predicate;

import java.util.Arrays;
import java.util.List;
import com.google.common.base.Predicate;

import javax.servlet.http.HttpServletRequest;

import org.springframework.cloud.netflix.zuul.util.NameUtils;
import org.springframework.tuple.Tuple;

/**
 * @author Spencer Gibb
 * @author Mustansar Anwar
 */
public class MethodRoutePredicateFactory extends RoutePredicateFactoryAbstract {

	public static final String METHOD_KEY = "method";

	@Override
	public List<String> argNames() {
		return Arrays.asList(METHOD_KEY);
	}

	@Override
	public Predicate<HttpServletRequest> apply(Tuple args) {
		final String method = args.getString(METHOD_KEY);

		return new Predicate<HttpServletRequest>() {
			public boolean apply(HttpServletRequest request) {
				return request.getMethod().matches(method);
			};
		};
	}

	@Override
	public String name() {
		return NameUtils.normalizePredicateName(getClass());
	}
}
