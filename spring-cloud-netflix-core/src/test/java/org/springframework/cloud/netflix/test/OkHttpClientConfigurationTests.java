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

import feign.Client;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockingDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.commons.httpclient.DefaultOkHttpClientConnectionPoolFactory;
import org.springframework.cloud.commons.httpclient.DefaultOkHttpClientFactory;
import org.springframework.cloud.commons.httpclient.OkHttpClientConnectionPoolFactory;
import org.springframework.cloud.commons.httpclient.OkHttpClientFactory;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.cloud.netflix.feign.ribbon.LoadBalancerFeignClient;
import org.springframework.cloud.netflix.ribbon.okhttp.OkHttpLoadBalancingClient;
import org.springframework.cloud.netflix.ribbon.support.RibbonRequestCustomizer;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandContext;
import org.springframework.cloud.netflix.zuul.filters.route.okhttp.OkHttpRibbonCommand;
import org.springframework.cloud.netflix.zuul.filters.route.okhttp.OkHttpRibbonCommandFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

/**
 * @author Ryan Baxter
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = OkHttpClientConfigurationTestApp.class, value = {"feign.okhttp.enabled: true",
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

	@Autowired
	OkHttpRibbonCommandFactory okHttpRibbonCommandFactory;

	@Test
	public void testFactories() {
		Assertions.assertThat(connectionPoolFactory).isInstanceOf(OkHttpClientConnectionPoolFactory.class);
		Assertions.assertThat(connectionPoolFactory).isInstanceOf(OkHttpClientConfigurationTestApp.MyOkHttpClientConnectionPoolFactory.class);
		Assertions.assertThat(okHttpClientFactory).isInstanceOf(OkHttpClientFactory.class);
		Assertions.assertThat(okHttpClientFactory).isInstanceOf(OkHttpClientConfigurationTestApp.MyOkHttpClientFactory.class);
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

	@Test
	public void testOkHttpLoadBalancingHttpClient() {
		RibbonCommandContext context = new RibbonCommandContext("foo"," GET", "http://localhost",
				false, new LinkedMultiValueMap<String, String>(), new LinkedMultiValueMap<String, String>(),
				null, new ArrayList<RibbonRequestCustomizer>(), 0l);
		OkHttpRibbonCommand command = okHttpRibbonCommandFactory.create(context);
		OkHttpLoadBalancingClient ribbonClient = command.getClient();
		OkHttpClient httpClient = getField(ribbonClient, "delegate");
		MockingDetails httpClientDetails = mockingDetails(httpClient);
		assertTrue(httpClientDetails.isMock());
	}

	protected <T> T getField(Object target, String name) {
		Field field = ReflectionUtils.findField(target.getClass(), name);
		ReflectionUtils.makeAccessible(field);
		Object value = ReflectionUtils.getField(field, target);
		return (T)value;
	}
}

@Configuration
@EnableAutoConfiguration
@RestController
@EnableZuulProxy
class OkHttpClientConfigurationTestApp {

	@RequestMapping
	public String index() {
		return "hello";
	}

	static class MyOkHttpClientConnectionPoolFactory extends DefaultOkHttpClientConnectionPoolFactory {
		@Override
		public ConnectionPool create(int maxIdleConnections, long keepAliveDuration, TimeUnit timeUnit) {
			return new ConnectionPool();
		}
	}

	static class MyOkHttpClientFactory extends DefaultOkHttpClientFactory {
	}

	@Configuration
	static class MyConfig {
		@Bean
		public OkHttpClientConnectionPoolFactory connectionPoolFactory() {
			return new MyOkHttpClientConnectionPoolFactory();
		}

		@Bean
		public OkHttpClientFactory clientFactory() {
			return new MyOkHttpClientFactory();
		}

		@Bean
		public OkHttpClient client() {
			return mock(OkHttpClient.class);
		}

	}

	@FeignClient(name="foo", serviceId = "foo")
	static interface FooClient {}
}
