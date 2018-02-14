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
 */
package org.springframework.cloud.netflix.ribbon.okhttp;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.loadbalancer.LoadBalancedBackOffPolicyFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryListenerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicyFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration;
import org.springframework.cloud.commons.httpclient.HttpClientConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonClientConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancedRetryPolicy;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancedRetryPolicyFactory;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerContext;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.netflix.client.ClientException;
import com.netflix.client.DefaultLoadBalancerRetryHandler;
import com.netflix.client.RetryHandler;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;

import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * @author Ryan Baxter
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(value = { "ribbon.okhttp.enabled: true", "ribbon.httpclient.enabled: false" })
@ContextConfiguration(classes = { RibbonAutoConfiguration.class,
		HttpClientConfiguration.class, RibbonClientConfiguration.class,
		LoadBalancerAutoConfiguration.class })
public class SpringRetryEnabledOkHttpClientTests implements ApplicationContextAware {

	private ApplicationContext context;
	private ILoadBalancer loadBalancer;
	private LoadBalancedBackOffPolicyFactory loadBalancedBackOffPolicyFactory = new LoadBalancedBackOffPolicyFactory.NoBackOffPolicyFactory();
	private LoadBalancedRetryListenerFactory loadBalancedRetryListenerFactory = new LoadBalancedRetryListenerFactory.DefaultRetryListenerFactory();
	
	@Test
	public void testLoadBalancedRetryFactoryBean() throws Exception {
		Map<String, LoadBalancedRetryPolicyFactory> factories = context
				.getBeansOfType(LoadBalancedRetryPolicyFactory.class);
		assertThat(factories.values(), hasSize(1));
		assertThat(factories.values().toArray()[0],
				instanceOf(RibbonLoadBalancedRetryPolicyFactory.class));
		Map<String, OkHttpLoadBalancingClient> clients = context
				.getBeansOfType(OkHttpLoadBalancingClient.class);
		assertThat(clients.values(), hasSize(1));
		assertThat(clients.values().toArray()[0],
				instanceOf(RetryableOkHttpLoadBalancingClient.class));
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}
	
	private RetryableOkHttpLoadBalancingClient setupClientForServerValidation(String serviceName, String host, int port,
			OkHttpClient delegate, ILoadBalancer lb) throws Exception {
		ServerIntrospector introspector = mock(ServerIntrospector.class);
		RetryHandler retryHandler = new DefaultLoadBalancerRetryHandler(1, 1, true);
		DefaultClientConfigImpl clientConfig = new DefaultClientConfigImpl();
		clientConfig.set(CommonClientConfigKey.OkToRetryOnAllOperations, true);
		clientConfig.set(CommonClientConfigKey.MaxAutoRetriesNextServer, 0);
		clientConfig.set(CommonClientConfigKey.MaxAutoRetries, 1);
		clientConfig.set(RibbonLoadBalancedRetryPolicy.RETRYABLE_STATUS_CODES, "");
		clientConfig.set(CommonClientConfigKey.IsSecure, false);
		clientConfig.setClientName(serviceName);
		RibbonLoadBalancerContext context = new RibbonLoadBalancerContext(lb, clientConfig, retryHandler);
		SpringClientFactory clientFactory = mock(SpringClientFactory.class);
		doReturn(context).when(clientFactory).getLoadBalancerContext(eq(serviceName));
		doReturn(clientConfig).when(clientFactory).getClientConfig(eq(serviceName));
		LoadBalancedRetryPolicyFactory factory = new RibbonLoadBalancedRetryPolicyFactory(clientFactory);
		RetryableOkHttpLoadBalancingClient client = new RetryableOkHttpLoadBalancingClient(delegate, clientConfig, introspector,
				factory, loadBalancedBackOffPolicyFactory, loadBalancedRetryListenerFactory);
		client.setLoadBalancer(lb);
		ReflectionTestUtils.setField(client, "delegate", delegate);
		return client;
	}
	
	@Test
	public void noServersFoundTest() throws Exception {
		String serviceName = "noservers";
		String host = serviceName;
		int port = 80;
		HttpMethod method = HttpMethod.POST;
		URI uri = new URI("http://" + host + ":" + port);
		OkHttpClient delegate = mock(OkHttpClient.class);
		ILoadBalancer lb = mock(ILoadBalancer.class);
		
		RetryableOkHttpLoadBalancingClient client = setupClientForServerValidation(serviceName, host, port, delegate, lb);
		OkHttpRibbonRequest request = mock(OkHttpRibbonRequest.class);
		doReturn(null).when(lb).chooseServer(eq(serviceName));
		doReturn(method).when(request).getMethod();
		doReturn(uri).when(request).getURI();
		doReturn(request).when(request).withNewUri(any(URI.class));
		Request okRequest = new Request.Builder().url("ws:testerror.sc").build();
		doReturn(okRequest).when(request).toRequest();
		try {
			client.execute(request, null);
			fail("Expected ClientException for no servers available");
		} catch (ClientException ex) {
			assertThat(ex.getMessage(), containsString("Load balancer does not have available server for client"));
		}
	}
	
	@Test
	public void invalidServerTest() throws Exception {
		String serviceName = "noservers";
		String host = serviceName;
		int port = 80;
		HttpMethod method = HttpMethod.POST;
		URI uri = new URI("http://" + host + ":" + port);
		OkHttpClient delegate = mock(OkHttpClient.class);
		ILoadBalancer lb = mock(ILoadBalancer.class);
		
		RetryableOkHttpLoadBalancingClient client = setupClientForServerValidation(serviceName, host, port, delegate, lb);
		OkHttpRibbonRequest request = mock(OkHttpRibbonRequest.class);
		doReturn(new Server(null,8000)).when(lb).chooseServer(eq(serviceName));
		doReturn(method).when(request).getMethod();
		doReturn(uri).when(request).getURI();
		doReturn(request).when(request).withNewUri(any(URI.class));
		Request okRequest = new Request.Builder().url("ws:testerror.sc").build();
		doReturn(okRequest).when(request).toRequest();

		try {
			client.execute(request, null);
			fail("Expected ClientException for no Invalid Host");
		} catch (ClientException ex) {
			assertThat(ex.getMessage(), containsString("Invalid Server for: "));
		}
	}
}
