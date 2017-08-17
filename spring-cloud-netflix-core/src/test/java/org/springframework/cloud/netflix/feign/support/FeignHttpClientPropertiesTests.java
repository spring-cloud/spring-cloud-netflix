/*
 *
 *  * Copyright 2013-2016 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.cloud.netflix.feign.support;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.util.EnvironmentTestUtils.addEnvironment;

/**
 * @author Ryan Baxter
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class FeignHttpClientPropertiesTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@After
	public void clear() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testDefaults() {
		setupContext();
		assertEquals(FeignHttpClientProperties.DEFAULT_CONNECTION_TIMEOUT, getProperties().getConnectionTimeout());
		assertEquals(FeignHttpClientProperties.DEFAULT_MAX_CONNECTIONS, getProperties().getMaxConnections());
		assertEquals(FeignHttpClientProperties.DEFAULT_MAX_CONNECTIONS_PER_ROUTE, getProperties().getMaxConnectionsPerRoute());
		assertEquals(FeignHttpClientProperties.DEFAULT_TIME_TO_LIVE, getProperties().getTimeToLive());
		assertEquals(FeignHttpClientProperties.DEFAULT_DISABLE_SSL_VALIDATION, getProperties().isDisableSslValidation());
		assertEquals(FeignHttpClientProperties.DEFAULT_FOLLOW_REDIRECTS, getProperties().isFollowRedirects());
	}

	@Test
	public void testCustomization() {
		addEnvironment(this.context, "feign.httpclient.maxConnections=2",
				"feign.httpclient.connectionTimeout=2",
				"feign.httpclient.maxConnectionsPerRoute=2",
				"feign.httpclient.timeToLive=2",
				"feign.httpclient.disableSslValidation=true",
				"feign.httpclient.followRedirects=false");
		setupContext();
		assertEquals(2, getProperties().getMaxConnections());
		assertEquals(2, getProperties().getConnectionTimeout());
		assertEquals(2, getProperties().getMaxConnectionsPerRoute());
		assertEquals(2L, getProperties().getTimeToLive());
		assertTrue(getProperties().isDisableSslValidation());
		assertFalse(getProperties().isFollowRedirects());
	}

	private void setupContext() {
		this.context.register(PropertyPlaceholderAutoConfiguration.class, TestConfiguration.class);
		this.context.refresh();
	}

	private FeignHttpClientProperties getProperties() {
		return this.context.getBean(FeignHttpClientProperties.class);
	}

	@Configuration
	@EnableConfigurationProperties
	protected static class TestConfiguration {
		@Bean
		FeignHttpClientProperties zuulProperties() {
			return new FeignHttpClientProperties() ;
		}
	}
}