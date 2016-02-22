/*
 * Copyright 2013-2016 the original author or authors.
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
 */

package org.springframework.cloud.netflix.zuul.filters.route;

import static org.junit.Assert.assertEquals;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.Test;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.Host;

/**
 * @author Andreas Kluth
 */
public class SimpleHostRoutingFilterTests {

	@Test
	public void connectionPropertiesAreApplied() {
		PoolingHttpClientConnectionManager newConnectionManager = createFilter(
				withHostProperties(100, 10)).newConnectionManager();

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

	private ZuulProperties withHostProperties(int maxPerHost, int maxPerRoute) {
		ZuulProperties properties = new ZuulProperties();
		properties.setHost(new Host(maxPerHost, maxPerRoute));
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
