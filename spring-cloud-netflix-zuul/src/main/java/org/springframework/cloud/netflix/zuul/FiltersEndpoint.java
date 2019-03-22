/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.zuul;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.filters.FilterRegistry;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

/**
 * Endpoint for listing Zuul filters.
 *
 * @author Daryl Robbins
 * @author Gregor Zurowski
 */
@Endpoint(id = "filters")
public class FiltersEndpoint {

	private final FilterRegistry filterRegistry;

	public FiltersEndpoint(FilterRegistry filterRegistry) {
		this.filterRegistry = filterRegistry;
	}

	@ReadOperation
	public Map<String, List<Map<String, Object>>> invoke() {
		// Map of filters by type
		final Map<String, List<Map<String, Object>>> filterMap = new TreeMap<>();

		for (ZuulFilter filter : this.filterRegistry.getAllFilters()) {
			// Ensure that we have a list to store filters of each type
			if (!filterMap.containsKey(filter.filterType())) {
				filterMap.put(filter.filterType(), new ArrayList<>());
			}

			final Map<String, Object> filterInfo = new LinkedHashMap<>();
			filterInfo.put("class", filter.getClass().getName());
			filterInfo.put("order", filter.filterOrder());
			filterInfo.put("disabled", filter.isFilterDisabled());
			filterInfo.put("static", filter.isStaticFilter());

			filterMap.get(filter.filterType()).add(filterInfo);
		}

		return filterMap;
	}

}
