/*
 * Copyright 2013-2017 the original author or authors.
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
 *
 */

package org.springframework.cloud.netflix.test;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockingDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.commons.httpclient.DefaultOkHttpClientConnectionPoolFactory;
import org.springframework.cloud.commons.httpclient.DefaultOkHttpClientFactory;
import org.springframework.cloud.commons.httpclient.OkHttpClientConnectionPoolFactory;
import org.springframework.cloud.commons.httpclient.OkHttpClientFactory;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.cloud.netflix.feign.ribbon.LoadBalancerFeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

import feign.Client;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

/**
 * @author Ryan Baxter
 */
@RunWith(SpringRunner.class)
@SpringBootTest(properties = {"feign.okhttp.enabled: true",
		"spring.cloud.httpclientfactories.ok.enabled: true", "ribbon.eureka.enabled = false", "ribbon.okhttp.enabled: true",
		"feign.okhttp.enabled: true", "ribbon.httpclient.enabled: false", "feign.httpclient.enabled: false"})
@DirtiesContext
public class OkHttpClientConfigurationTests {

	@Autowired
	OkHttpClientFactory okHttpClientFactory;

	@Autowired
	OkHttpClientConnectionPoolFactory connectionPoolFactory;

	@Autowired
	LoadBalancerFeignClient feignClient;

	@Test
	public void testFactories() {
		assertThat(connectionPoolFactory).isInstanceOf(OkHttpClientConnectionPoolFactory.class);
		assertThat(connectionPoolFactory).isInstanceOf(TestConfig.MyOkHttpClientConnectionPoolFactory.class);
		assertThat(okHttpClientFactory).isInstanceOf(OkHttpClientFactory.class);
		assertThat(okHttpClientFactory).isInstanceOf(TestConfig.MyOkHttpClientFactory.class);
	}

	@Test
	public void testHttpClientWithFeign() {
		Client delegate = feignClient.getDelegate();
		assertTrue(feign.okhttp.OkHttpClient.class.isInstance(delegate));
		feign.okhttp.OkHttpClient okHttpClient = (feign.okhttp.OkHttpClient)delegate;
		OkHttpClient httpClient = getField(okHttpClient, "delegate");
		MockingDetails httpClientDetails = mockingDetails(httpClient);
		assertTrue(httpClientDetails.isMock());
	}

	protected <T> T getField(Object target, String name) {
		Field field = ReflectionUtils.findField(target.getClass(), name);
		ReflectionUtils.makeAccessible(field);
		Object value = ReflectionUtils.getField(field, target);
		return (T)value;
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	static class TestConfig {

		static class MyOkHttpClientConnectionPoolFactory extends DefaultOkHttpClientConnectionPoolFactory {
			@Override
			public ConnectionPool create(int maxIdleConnections, long keepAliveDuration, TimeUnit timeUnit) {
				return new ConnectionPool();
			}
		}

		static class MyOkHttpClientFactory extends DefaultOkHttpClientFactory {
			public MyOkHttpClientFactory(OkHttpClient.Builder builder) {
				super(builder);
			}
		}

		@Bean
		public OkHttpClientConnectionPoolFactory connectionPoolFactory() {
			return new MyOkHttpClientConnectionPoolFactory();
		}

		@Bean
		public OkHttpClientFactory clientFactory(OkHttpClient.Builder builder) {
			return new MyOkHttpClientFactory(builder);
		}

		@Bean
		public OkHttpClient client() {
											   return mock(OkHttpClient.class);
																			   }
	}

	@FeignClient(name="foo", serviceId = "foo")
	interface FooClient {}
}
