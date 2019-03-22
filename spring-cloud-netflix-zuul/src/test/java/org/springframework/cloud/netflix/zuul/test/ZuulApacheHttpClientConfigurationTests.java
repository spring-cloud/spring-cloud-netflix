/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.zuul.test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;

import org.apache.http.Header;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockingDetails;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientFactory;
import org.springframework.cloud.commons.httpclient.DefaultApacheHttpClientFactory;
import org.springframework.cloud.netflix.ribbon.apache.RibbonLoadBalancingHttpClient;
import org.springframework.cloud.netflix.ribbon.support.RibbonCommandContext;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.filters.route.SimpleHostRoutingFilter;
import org.springframework.cloud.netflix.zuul.filters.route.apache.HttpClientRibbonCommand;
import org.springframework.cloud.netflix.zuul.filters.route.apache.HttpClientRibbonCommandFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

/**
 * @author Ryan Baxter
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(properties = { "ribbon.eureka.enabled = false" })
@DirtiesContext
public class ZuulApacheHttpClientConfigurationTests {

	@Autowired
	SimpleHostRoutingFilter simpleHostRoutingFilter;

	@Autowired
	HttpClientRibbonCommandFactory httpClientRibbonCommandFactory;

	@Test
	public void testHttpClientSimpleHostRoutingFilter() {
		CloseableHttpClient httpClient = getField(simpleHostRoutingFilter, "httpClient");
		MockingDetails httpClientDetails = mockingDetails(httpClient);
		assertThat(httpClientDetails.isMock());
	}

	@Test
	public void testRibbonLoadBalancingHttpClient() {
		RibbonCommandContext context = new RibbonCommandContext("foo", " GET",
				"http://localhost", false, new LinkedMultiValueMap<>(),
				new LinkedMultiValueMap<>(), null, new ArrayList<>(), 0L);
		HttpClientRibbonCommand command = httpClientRibbonCommandFactory.create(context);
		RibbonLoadBalancingHttpClient ribbonClient = command.getClient();
		CloseableHttpClient httpClient = getField(ribbonClient, "delegate");
		MockingDetails httpClientDetails = mockingDetails(httpClient);
		assertThat(httpClientDetails.isMock());
	}

	@SuppressWarnings("unchecked")
	protected <T> T getField(Object target, String name) {
		Field field = ReflectionUtils.findField(target.getClass(), name);
		ReflectionUtils.makeAccessible(field);
		Object value = ReflectionUtils.getField(field, target);
		return (T) value;
	}

	@Bean
	public ApacheHttpClientFactory apacheHttpClientFactory(HttpClientBuilder builder) {
		return new TestConfig.MyApacheHttpClientFactory(builder);
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EnableZuulProxy
	static class TestConfig {

		static class MyApacheHttpClientFactory extends DefaultApacheHttpClientFactory {

			MyApacheHttpClientFactory(HttpClientBuilder builder) {
				super(builder);
			}

			@Override
			public HttpClientBuilder createBuilder() {
				CloseableHttpClient client = mock(CloseableHttpClient.class);
				CloseableHttpResponse response = mock(CloseableHttpResponse.class);
				StatusLine statusLine = mock(StatusLine.class);
				doReturn(200).when(statusLine).getStatusCode();
				Mockito.doReturn(statusLine).when(response).getStatusLine();
				Header[] headers = new BasicHeader[0];
				doReturn(headers).when(response).getAllHeaders();
				try {
					Mockito.doReturn(response).when(client)
							.execute(any(HttpUriRequest.class));
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				HttpClientBuilder builder = mock(HttpClientBuilder.class);
				Mockito.doReturn(client).when(builder).build();
				return builder;
			}

		}

	}

}
