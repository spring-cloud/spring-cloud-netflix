package org.springframework.cloud.netflix.zuul.filters.route;

import static org.junit.Assert.assertEquals;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.Test;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.HostRoutingFilterProperties;

/**
 * @author Andreas Kluth
 */
public class SimpleHostRoutingFilterTests {

	@Test
	public void connectionPropertiesAreApplied() {
		PoolingHttpClientConnectionManager newConnectionManager = createFilter(
				withProperties(100, 10)).newConnectionManager();

		assertEquals(100, newConnectionManager.getMaxTotal());
		assertEquals(10, newConnectionManager.getDefaultMaxPerRoute());
	}

	@Test
	public void defaultPropertiesAreApplied() {
		PoolingHttpClientConnectionManager newConnectionManager = createFilter(
				new ZuulProperties()).newConnectionManager();

		assertEquals(200, newConnectionManager.getMaxTotal());
		assertEquals(20, newConnectionManager.getDefaultMaxPerRoute());
	}

	private ZuulProperties withProperties(int maxPerHost, int maxPerRoute) {
		ZuulProperties properties = new ZuulProperties();
		properties.setHostRoutingFilter(
				new HostRoutingFilterProperties(maxPerHost, maxPerRoute));
		return properties;
	}

	private SpySimpleHostRoutingFilter createFilter(ZuulProperties properties) {
		return new SpySimpleHostRoutingFilter(properties);
	}

	private static class SpySimpleHostRoutingFilter extends SimpleHostRoutingFilter {

		public SpySimpleHostRoutingFilter(ZuulProperties properties) {
			super(new ProxyRequestHelper(), properties);
		}

		@Override
		public PoolingHttpClientConnectionManager newConnectionManager() {
			return super.newConnectionManager();
		}

	}
}
