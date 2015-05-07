package org.springframework.cloud.netflix.zuul;

import com.netflix.zuul.ZuulFilter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.cloud.netflix.zuul.ZuulConfiguration.ZuulFilterConfiguration;

/**
 * Corresponding unit test class for class {@link ZuulFilterConfigurationTest}.
 */
public class ZuulFilterConfigurationTest {


    private Map<String, ZuulFilter> filters = new HashMap<>();

    @Mock
    private ZuulProperties zuulProperties;

    private ZuulFilterConfiguration zuulFilterConfiguration = new ZuulFilterConfiguration();

    @Before
    public void init() {
        initMocks(this);
        filters.put("test", sampleFilter());
        filters.put("test2", sampleFilter());
        zuulFilterConfiguration = new ZuulFilterConfiguration();
        zuulFilterConfiguration.setFilters(filters);
        zuulFilterConfiguration.setZuulProperties(zuulProperties);
    }

    public ZuulFilter sampleFilter() {
        return new ZuulFilter() {
            @Override
            public String filterType() {
                return "pre";
            }

            @Override
            public boolean shouldFilter() {
                return true;
            }

            @Override
            public Object run() {
                return null;
            }

            @Override
            public int filterOrder() {
                return 0;
            }
        };
    }

    @Test
    public void testSuccessIgnoreOneFilter() {
        assertEquals(filters.size(), 2);
        List<String> ignoredFilters = new ArrayList<>();
        ignoredFilters.add("test");
        when(zuulProperties.getIgnoredFilters()).thenReturn(ignoredFilters);
        zuulFilterConfiguration.zuulFilterInitializer();
        assertEquals(filters.size(), 1);
        assertNotNull(filters.get("test2"));
    }

    @Test
    public void testNotFoudIgnoreFilter() {
        assertEquals(filters.size(), 2);
        List<String> ignoredFilters = new ArrayList<>();
        ignoredFilters.add("notfound");
        when(zuulProperties.getIgnoredFilters()).thenReturn(ignoredFilters);
        zuulFilterConfiguration.zuulFilterInitializer();
        assertEquals(filters.size(), 2);
        assertNotNull(filters.get("test"));
        assertNotNull(filters.get("test2"));
    }

    @Test
    public void testNullEntryIgnoreFilter() {
        assertEquals(filters.size(), 2);
        List<String> ignoredFilters = new ArrayList<>();
        ignoredFilters.add(null);
        when(zuulProperties.getIgnoredFilters()).thenReturn(ignoredFilters);
        zuulFilterConfiguration.zuulFilterInitializer();
        assertEquals(filters.size(), 2);
        assertNotNull(filters.get("test"));
        assertNotNull(filters.get("test2"));
    }

    @Test
    public void testDubleItemisIgnoreFilter() {
        assertEquals(filters.size(), 2);
        List<String> ignoredFilters = new ArrayList<>();
        ignoredFilters.add("test");
        ignoredFilters.add("test");
        when(zuulProperties.getIgnoredFilters()).thenReturn(ignoredFilters);
        zuulFilterConfiguration.zuulFilterInitializer();
        assertEquals(filters.size(), 1);
        assertNotNull(filters.get("test2"));
    }

    @Test
    public void testNullCheckIgnoreList() {
        List<String> ignoredFilters = null;
        when(zuulProperties.getIgnoredFilters()).thenReturn(ignoredFilters);
        zuulFilterConfiguration.zuulFilterInitializer();
        assertEquals(filters.size(), 2);
    }
}
