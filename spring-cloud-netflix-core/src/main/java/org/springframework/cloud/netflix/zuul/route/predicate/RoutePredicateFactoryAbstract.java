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

import java.util.Collections;
import java.util.List;
import com.google.common.base.Predicate;

import javax.servlet.http.HttpServletRequest;

import org.springframework.cloud.netflix.zuul.util.NameUtils;
import org.springframework.tuple.Tuple;
import org.springframework.util.Assert;

/**
 * @author Mustansar Anwar
 */
public abstract class RoutePredicateFactoryAbstract
		implements RoutePredicateFactory {

	@Override
	public List<String> argNames() {
		return Collections.emptyList();
	}

	@Override
	public boolean validateArgs() {
		return true;
	}

	@Override
	public void validate(int requiredSize, Tuple args) {
		Assert.isTrue(args != null && args.size() == requiredSize,
				"args must have " + requiredSize + " entry(s)");
	}

	@Override
	public void validateMin(int minSize, Tuple args) {
		Assert.isTrue(args != null && args.size() >= minSize,
				"args must have at least " + minSize + " entry(s)");
	}

	@Override
	public String name() {
		return NameUtils.normalizePredicateName(getClass());
	}

	@Override
	public abstract Predicate<HttpServletRequest> apply(Tuple args);

}
