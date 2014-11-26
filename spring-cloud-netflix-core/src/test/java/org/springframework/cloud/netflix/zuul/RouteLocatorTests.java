package org.springframework.cloud.netflix.zuul;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.env.ConfigurableEnvironment;

import com.google.common.collect.Lists;

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
		ZuulProperties properties = new ZuulProperties();
		RouteLocator routeLocator = new RouteLocator(this.discovery, properties);
		properties.setIgnoredServices(Lists.newArrayList(IGNOREDSERVICE));
		properties.getRoute().put(ASERVICE, "/"+ASERVICE + "/**");

		when(discovery.getServices()).thenReturn(
				Lists.newArrayList(MYSERVICE, IGNOREDSERVICE));

		Map<String, String> routesMap = routeLocator.getRoutes();

		assertNotNull("routesMap was null", routesMap);
		assertFalse("routesMap was empty", routesMap.isEmpty());
		assertMapping(routesMap, MYSERVICE);
		assertMapping(routesMap, ASERVICE);

		String serviceId = routesMap.get(getMapping(IGNOREDSERVICE));
		assertNull("routes did not ignore " + IGNOREDSERVICE, serviceId);
	}

	protected void assertMapping(Map<String, String> routesMap, String expectedServiceId) {
		String mapping = getMapping(expectedServiceId);
		String serviceId = routesMap.get(mapping);
		assertEquals("routesMap had wrong value for " + mapping, expectedServiceId,
				serviceId);
	}

	private String getMapping(String serviceId) {
		return "/" + serviceId + "/**";
	}
}
