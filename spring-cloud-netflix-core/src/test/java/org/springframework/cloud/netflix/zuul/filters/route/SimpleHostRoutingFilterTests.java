package org.springframework.cloud.netflix.zuul.filters.route;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Andreas Kluth
 */
public class SimpleHostRoutingFilterTests {

	private static final String ZUUL_MAX_HOST_CONNECTIONS = "zuul.max.host.connections";
	private static final String ZUUL_MAX_ROUTE_CONNECTIONS = "zuul.max.route.connections";

	@Before
	@After
	public void reset() {
		Properties sysProps = System.getProperties();
		sysProps.remove(ZUUL_MAX_HOST_CONNECTIONS);
		sysProps.remove(ZUUL_MAX_ROUTE_CONNECTIONS);
	}

	@Test
	public void maxPerRouteAndMaxTotalDiffer() {
		setHostAndRouteConnectionProperties("100", "10");

		PoolingHttpClientConnectionManager newConnectionManager = createFilter()
				.newConnectionManager();

		assertEquals(100, newConnectionManager.getMaxTotal());
		assertEquals(10, newConnectionManager.getDefaultMaxPerRoute());
	}

	@Test
	public void defaults() {
		PoolingHttpClientConnectionManager newConnectionManager = createFilter()
				.newConnectionManager();
		assertEquals(200, newConnectionManager.getMaxTotal());
		assertEquals(20, newConnectionManager.getDefaultMaxPerRoute());
	}

	private void setHostAndRouteConnectionProperties(String maxPerHost,
			String maxPerRoute) {
		System.setProperty(ZUUL_MAX_HOST_CONNECTIONS, maxPerHost);
		System.setProperty(ZUUL_MAX_ROUTE_CONNECTIONS, maxPerRoute);
	}

	private SpySimpleHostRoutingFilter createFilter() {

		return new SpySimpleHostRoutingFilter();
	}

	private static class SpySimpleHostRoutingFilter extends SimpleHostRoutingFilter {

		@Override
		public PoolingHttpClientConnectionManager newConnectionManager() {
			return super.newConnectionManager();
		}

	}
}
