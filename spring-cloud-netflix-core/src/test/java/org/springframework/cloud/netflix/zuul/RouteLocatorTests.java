package org.springframework.cloud.netflix.zuul;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.mock.env.MockPropertySource;

import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Spencer Gibb
 */
public class RouteLocatorTests {

    public static final String IGNOREDSERVICE = "ignoredservice";
    public static final String ASERVICE = "aservice";
    public static final String MYSERVICE = "myservice";
    @Mock
    ConfigurableEnvironment env;

    @Mock
    DiscoveryClient discovery;


    @Before
    public void init() {
        initMocks(this);
    }

    @Test
    public void testGetRoutes() {
        RouteLocator routeLocator = new RouteLocator();
        routeLocator.properties = new ZuulProperties();
        routeLocator.properties.setIgnoredServices(Lists.newArrayList(IGNOREDSERVICE));
        routeLocator.discovery = this.discovery;
        routeLocator.env = this.env;

        MutablePropertySources propertySources = new MutablePropertySources();
        propertySources.addFirst(new MockPropertySource().withProperty("zuul.route."+ ASERVICE, getMapping(ASERVICE)));
        when(env.getPropertySources()).thenReturn(propertySources);
        when(discovery.getServices()).thenReturn(Lists.newArrayList(MYSERVICE, IGNOREDSERVICE));

        Map<String, String> routesMap = routeLocator.getRoutes();

        assertNotNull("routesMap was null", routesMap);
        assertFalse("routesMap was empty", routesMap.isEmpty());
        assertMapping(routesMap, MYSERVICE);
        assertMapping(routesMap, ASERVICE);

        String serviceId = routesMap.get(getMapping(IGNOREDSERVICE));
        assertNull("routes did not ignore "+IGNOREDSERVICE, serviceId);
    }

    protected void assertMapping(Map<String, String> routesMap, String expectedServiceId) {
        String mapping = getMapping(expectedServiceId);
        String serviceId = routesMap.get(mapping);
        assertEquals("routesMap had wrong value for "+mapping, expectedServiceId, serviceId);
    }

    private String getMapping(String serviceId) {
        return "/"+ serviceId +"/**";
    }
}
