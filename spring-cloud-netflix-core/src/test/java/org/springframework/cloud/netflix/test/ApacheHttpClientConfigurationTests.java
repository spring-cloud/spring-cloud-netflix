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

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import org.apache.http.Header;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockingDetails;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientConnectionManagerFactory;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientFactory;
import org.springframework.cloud.commons.httpclient.DefaultApacheHttpClientConnectionManagerFactory;
import org.springframework.cloud.commons.httpclient.DefaultApacheHttpClientFactory;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.cloud.netflix.feign.ribbon.LoadBalancerFeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

import feign.Client;
import feign.httpclient.ApacheHttpClient;

/**
 * @author Ryan Baxter
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(properties =
		{"feign.okhttp.enabled: false",
		"ribbon.eureka.enabled = false"})
@DirtiesContext
public class ApacheHttpClientConfigurationTests {

	@Autowired
	ApacheHttpClientConnectionManagerFactory connectionManagerFactory;

	@Autowired
	ApacheHttpClientFactory httpClientFactory;

	@Autowired
	LoadBalancerFeignClient feignClient;

	@Test
	public void testFactories() {
		assertThat(connectionManagerFactory).isInstanceOf(ApacheHttpClientConnectionManagerFactory.class);
		assertThat(connectionManagerFactory).isInstanceOf(ApacheHttpClientConfigurationTestApp.MyApacheHttpClientConnectionManagerFactory.class);
		assertThat(httpClientFactory).isInstanceOf(ApacheHttpClientFactory.class);
		assertThat(httpClientFactory).isInstanceOf(ApacheHttpClientConfigurationTestApp.MyApacheHttpClientFactory.class);
	}

	@Test
	public void testHttpClientWithFeign() {
		Client delegate = feignClient.getDelegate();
		assertTrue(ApacheHttpClient.class.isInstance(delegate));
		ApacheHttpClient apacheHttpClient = (ApacheHttpClient)delegate;
		HttpClient httpClient = getField(apacheHttpClient, "client");
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
	@EnableFeignClients(clients = {ApacheHttpClientConfigurationTestApp.FooClient.class})
	static class ApacheHttpClientConfigurationTestApp {

		static class MyApacheHttpClientConnectionManagerFactory extends DefaultApacheHttpClientConnectionManagerFactory {
			@Override
			public HttpClientConnectionManager newConnectionManager(boolean disableSslValidation, int maxTotalConnections, int maxConnectionsPerRoute, long timeToLive, TimeUnit timeUnit, RegistryBuilder registry) {
				return mock(PoolingHttpClientConnectionManager.class);
			}
		}

		static class MyApacheHttpClientFactory extends DefaultApacheHttpClientFactory {
			public MyApacheHttpClientFactory(HttpClientBuilder builder) {
				super(builder);
			}

			@Override
			public HttpClientBuilder createBuilder() {
				CloseableHttpClient client =  mock(CloseableHttpClient.class);
				CloseableHttpResponse response = mock(CloseableHttpResponse.class);
				StatusLine statusLine = mock(StatusLine.class);
				doReturn(200).when(statusLine).getStatusCode();
				Mockito.doReturn(statusLine).when(response).getStatusLine();
				Header[] headers = new BasicHeader[0];
				doReturn(headers).when(response).getAllHeaders();
				try {
					Mockito.doReturn(response).when(client).execute(any(HttpUriRequest.class));
				} catch (IOException e) {
					e.printStackTrace();
				}
				HttpClientBuilder builder = mock(HttpClientBuilder.class);
				Mockito.doReturn(client).when(builder).build();
				return builder;
			}
		}

		@Configuration
		static class MyConfig {

			@Bean
			public ApacheHttpClientFactory apacheHttpClientFactory(HttpClientBuilder builder) {
				return new MyApacheHttpClientFactory(builder);
			}

			@Bean
			public ApacheHttpClientConnectionManagerFactory connectionManagerFactory() {
				return new MyApacheHttpClientConnectionManagerFactory();
			}

		}

		@FeignClient(name="foo", serviceId = "foo")
		interface FooClient {}
	}

}

