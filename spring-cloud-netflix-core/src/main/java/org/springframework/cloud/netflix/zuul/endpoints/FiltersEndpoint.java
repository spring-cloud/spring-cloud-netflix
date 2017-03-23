package org.springframework.cloud.netflix.zuul.endpoints;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.filters.FilterRegistry;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

/**
 * Endpoint for listing Zuul filters
 */
@ManagedResource(description = "List Zuul filters")
public class FiltersEndpoint implements MvcEndpoint {

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    @ManagedAttribute
    public Map<String, List<Map<String, Object>>> getFilters() {
        final FilterRegistry filterRegistry = FilterRegistry.instance();

        // Map of filters by type
        final Map<String, List<Map<String, Object>>> filterMap =
                new TreeMap<String, List<Map<String, Object>>>();

        for (ZuulFilter f : filterRegistry.getAllFilters()) {

            // Ensure that we have a list to store filters of each type
            if (!filterMap.containsKey(f.filterType())) {
                filterMap.put(f.filterType(), new ArrayList<Map<String, Object>>());
            }


            final Map<String, Object> filterInfo = new LinkedHashMap<String, Object>();

            filterInfo.put("class", f.getClass().getName());
            filterInfo.put("order", f.filterOrder());
            filterInfo.put("disabled", f.isFilterDisabled());
            filterInfo.put("static", f.isStaticFilter());

            filterMap.get(f.filterType()).add(filterInfo);
        }

        return filterMap;
    }

    @Override
    public String getPath() {
        return "/filters";
    }

    @Override
    public boolean isSensitive() {
        return true;
    }

    @Override
    public Class<? extends Endpoint<?>> getEndpointType() {
        return null;
    }

}
