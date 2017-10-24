package org.springframework.cloud.netflix.zuul;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.filters.FilterRegistry;

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
