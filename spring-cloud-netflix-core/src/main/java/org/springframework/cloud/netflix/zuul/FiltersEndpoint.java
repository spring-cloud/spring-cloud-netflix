package org.springframework.cloud.netflix.zuul;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.filters.FilterRegistry;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Endpoint for listing Zuul filters.
 *
 * @author Daryl Robbins
 * @author Gregor Zurowski
 */
@ManagedResource(description = "List Zuul filters")
public class FiltersEndpoint extends AbstractEndpoint<Map<String, List<Map<String, Object>>>> {

	private static final String ID = "filters";

	private final FilterRegistry filterRegistry;

	public FiltersEndpoint(FilterRegistry filterRegistry) {
		super(ID, true);
		this.filterRegistry = filterRegistry;
	}

	@ManagedAttribute
	@Override
	public Map<String, List<Map<String, Object>>> invoke() {
		// Map of filters by type
		final Map<String, List<Map<String, Object>>> filterMap = new TreeMap<>();

		for (ZuulFilter filter : this.filterRegistry.getAllFilters()) {
			// Ensure that we have a list to store filters of each type
			if (!filterMap.containsKey(filter.filterType())) {
				filterMap.put(filter.filterType(), new ArrayList<Map<String, Object>>());
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
