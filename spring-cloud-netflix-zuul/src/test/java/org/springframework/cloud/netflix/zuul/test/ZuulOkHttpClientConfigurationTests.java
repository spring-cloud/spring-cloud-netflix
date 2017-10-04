/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.zuul.test;

import java.lang.reflect.Field;
import java.util.ArrayList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockingDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.commons.httpclient.OkHttpClientConnectionPoolFactory;
import org.springframework.cloud.commons.httpclient.OkHttpClientFactory;
import org.springframework.cloud.netflix.ribbon.okhttp.OkHttpLoadBalancingClient;
import org.springframework.cloud.netflix.ribbon.support.RibbonCommandContext;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.filters.route.okhttp.OkHttpRibbonCommand;
import org.springframework.cloud.netflix.zuul.filters.route.okhttp.OkHttpRibbonCommandFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.ReflectionUtils;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

import okhttp3.OkHttpClient;

/**
 * @author Ryan Baxter
 */
@RunWith(SpringRunner.class)
@SpringBootTest(properties = {
		"spring.cloud.httpclientfactories.ok.enabled: true",
		"ribbon.eureka.enabled = false", "ribbon.okhttp.enabled: true",
		"ribbon.httpclient.enabled: false" })
@DirtiesContext
public class ZuulOkHttpClientConfigurationTests {

	@Autowired
	OkHttpClientFactory okHttpClientFactory;

	@Autowired
	OkHttpClientConnectionPoolFactory connectionPoolFactory;

	@Autowired
	OkHttpRibbonCommandFactory okHttpRibbonCommandFactory;

	@Test
	public void testOkHttpLoadBalancingHttpClient() {
		RibbonCommandContext context = new RibbonCommandContext("foo", " GET",
				"http://localhost", false, new LinkedMultiValueMap<>(),
				new LinkedMultiValueMap<>(), null,
				new ArrayList<>(), 0l);
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
		return (T) value;
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EnableZuulProxy
	static class TestConfig {
		@Bean
		public OkHttpClient client() {
			return mock(OkHttpClient.class);
		}

	}
}
