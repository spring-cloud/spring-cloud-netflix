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
import feign.httpclient.ApacheHttpClient;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
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
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockingDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientConnectionManagerFactory;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientFactory;
import org.springframework.cloud.commons.httpclient.DefaultApacheHttpClientConnectionManagerFactory;
import org.springframework.cloud.commons.httpclient.DefaultApacheHttpClientFactory;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.cloud.netflix.feign.ribbon.LoadBalancerFeignClient;
import org.springframework.cloud.netflix.ribbon.apache.RibbonLoadBalancingHttpClient;
import org.springframework.cloud.netflix.ribbon.support.RibbonRequestCustomizer;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandContext;
import org.springframework.cloud.netflix.zuul.filters.route.SimpleHostRoutingFilter;
import org.springframework.cloud.netflix.zuul.filters.route.apache.HttpClientRibbonCommand;
import org.springframework.cloud.netflix.zuul.filters.route.apache.HttpClientRibbonCommandFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

/**
 * @author Ryan Baxter
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ApacheHttpClientConfigurationTestApp.class, value = {"feign.okhttp.enabled: false",
		"ribbon.eureka.enabled = false"})
@DirtiesContext
public class ApacheHttpClientConfigurationTests {

	@Autowired
	ApacheHttpClientConnectionManagerFactory connectionManagerFactory;

	@Autowired
	ApacheHttpClientFactory httpClientFactory;

	@Autowired
	SimpleHostRoutingFilter simpleHostRoutingFilter;

	@Autowired
	LoadBalancerFeignClient feignClient;

	@Autowired
	HttpClientRibbonCommandFactory httpClientRibbonCommandFactory;

	@Test
	public void testFactories() {
		Assertions.assertThat(connectionManagerFactory).isInstanceOf(ApacheHttpClientConnectionManagerFactory.class);
		Assertions.assertThat(connectionManagerFactory).isInstanceOf(ApacheHttpClientConfigurationTestApp.MyApacheHttpClientConnectionManagerFactory.class);
		Assertions.assertThat(httpClientFactory).isInstanceOf(ApacheHttpClientFactory.class);
		Assertions.assertThat(httpClientFactory).isInstanceOf(ApacheHttpClientConfigurationTestApp.MyApacheHttpClientFactory.class);
	}

	@Test
	public void testHttpClientSimpleHostRoutingFilter() {
		CloseableHttpClient httpClient = getField(simpleHostRoutingFilter, "httpClient");
		MockingDetails httpClientDetails = mockingDetails(httpClient);
		assertTrue(httpClientDetails.isMock());
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

	@Test
	public void testRibbonLoadBalancingHttpClient() {
		RibbonCommandContext context = new RibbonCommandContext("foo"," GET", "http://localhost",
				false, new LinkedMultiValueMap<String, String>(), new LinkedMultiValueMap<String, String>(),
				null, new ArrayList<RibbonRequestCustomizer>(), 0l);
		HttpClientRibbonCommand command = httpClientRibbonCommandFactory.create(context);
		RibbonLoadBalancingHttpClient ribbonClient = command.getClient();
		CloseableHttpClient httpClient = getField(ribbonClient, "delegate");
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
@EnableFeignClients(clients = {ApacheHttpClientConfigurationTestApp.FooClient.class})
@EnableZuulProxy
class ApacheHttpClientConfigurationTestApp {

	@RequestMapping
	public String index() {
		return "hello";
	}

	static class MyApacheHttpClientConnectionManagerFactory extends DefaultApacheHttpClientConnectionManagerFactory {
		@Override
		public HttpClientConnectionManager newConnectionManager(boolean disableSslValidation, int maxTotalConnections, int maxConnectionsPerRoute, long timeToLive, TimeUnit timeUnit, RegistryBuilder registry) {
			return mock(PoolingHttpClientConnectionManager.class);
		}
	}

	static class MyApacheHttpClientFactory extends DefaultApacheHttpClientFactory {
		@Override
		public HttpClientBuilder createBuilder() {
			CloseableHttpClient client =  mock(CloseableHttpClient.class);
			CloseableHttpResponse response = mock(CloseableHttpResponse.class);
			StatusLine statusLine = mock(StatusLine.class);
			doReturn(200).when(statusLine).getStatusCode();
			doReturn(statusLine).when(response).getStatusLine();
			Header[] headers = new BasicHeader[0];
			doReturn(headers).when(response).getAllHeaders();
			try {
				doReturn(response).when(client).execute(any(HttpUriRequest.class));
			} catch (IOException e) {
				e.printStackTrace();
			}
			HttpClientBuilder builder = mock(HttpClientBuilder.class);
			doReturn(client).when(builder).build();
			return builder;
		}
	}

	@Configuration
	static class MyConfig {

		@Bean
		public ApacheHttpClientFactory apacheHttpClientFactory() {
			return new MyApacheHttpClientFactory();
		}

		@Bean
		public ApacheHttpClientConnectionManagerFactory connectionManagerFactory() {
			return new MyApacheHttpClientConnectionManagerFactory();
		}

	}

	@FeignClient(name="foo", serviceId = "foo")
	static interface FooClient {}
}


