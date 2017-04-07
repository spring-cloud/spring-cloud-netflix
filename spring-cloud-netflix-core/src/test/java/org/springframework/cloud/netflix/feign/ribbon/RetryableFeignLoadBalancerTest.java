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

package org.springframework.cloud.netflix.feign.ribbon;

import feign.Client;
import feign.Request;
import feign.Response;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicy;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicyFactory;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;
import org.springframework.cloud.netflix.ribbon.DefaultServerIntrospector;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancedRetryPolicy;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancedRetryPolicyFactory;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerContext;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.http.HttpRequest;

import com.netflix.client.RequestSpecificRetryHandler;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;

import static com.netflix.client.config.CommonClientConfigKey.ConnectTimeout;
import static com.netflix.client.config.CommonClientConfigKey.MaxAutoRetries;
import static com.netflix.client.config.CommonClientConfigKey.MaxAutoRetriesNextServer;
import static com.netflix.client.config.CommonClientConfigKey.OkToRetryOnAllOperations;
import static com.netflix.client.config.CommonClientConfigKey.ReadTimeout;
import static com.netflix.client.config.DefaultClientConfigImpl.DEFAULT_MAX_AUTO_RETRIES;
import static com.netflix.client.config.DefaultClientConfigImpl.DEFAULT_MAX_AUTO_RETRIES_NEXT_SERVER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Ryan Baxter
 */
public class RetryableFeignLoadBalancerTest {
	@Mock
	private ILoadBalancer lb;
	@Mock
	private IClientConfig config;
	private ServerIntrospector inspector = new DefaultServerIntrospector();

	private Integer defaultConnectTimeout = 10000;
	private Integer defaultReadTimeout = 10000;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		when(this.config.get(MaxAutoRetries, DEFAULT_MAX_AUTO_RETRIES)).thenReturn(1);
		when(this.config.get(MaxAutoRetriesNextServer,
				DEFAULT_MAX_AUTO_RETRIES_NEXT_SERVER)).thenReturn(1);
		when(this.config.get(OkToRetryOnAllOperations, eq(anyBoolean())))
				.thenReturn(true);
		when(this.config.get(ConnectTimeout)).thenReturn(this.defaultConnectTimeout);
		when(this.config.get(ReadTimeout)).thenReturn(this.defaultReadTimeout);
	}

	@Test
	public void executeNoFailure() throws Exception {
		RibbonLoadBalancerContext lbContext = new RibbonLoadBalancerContext(lb, config);
		SpringClientFactory clientFactory = mock(SpringClientFactory.class);
		doReturn(lbContext).when(clientFactory).getLoadBalancerContext(any(String.class));
		IClientConfig config = mock(IClientConfig.class);
		doReturn(1).when(config).get(eq(CommonClientConfigKey.MaxAutoRetries), anyInt());
		doReturn(1).when(config).get(eq(CommonClientConfigKey.MaxAutoRetriesNextServer), anyInt());
		doReturn(true).when(config).get(eq(CommonClientConfigKey.OkToRetryOnAllOperations), eq(false));
		doReturn(defaultConnectTimeout).when(config).get(eq(CommonClientConfigKey.ConnectTimeout));
		doReturn(defaultReadTimeout).when(config).get(eq(CommonClientConfigKey.ReadTimeout));
		doReturn("404,502,foo, ,").when(config).getPropertyAsString(eq(RibbonLoadBalancedRetryPolicy.RETRYABLE_STATUS_CODES),eq(""));
		doReturn(config).when(clientFactory).getClientConfig(eq("default"));
		RibbonLoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory = new RibbonLoadBalancedRetryPolicyFactory(clientFactory);
		HttpRequest springRequest = mock(HttpRequest.class);
		Request feignRequest = Request.create("GET", "http://foo", new HashMap<String, Collection<String>>(),
				new byte[]{}, StandardCharsets.UTF_8);
		Client client = mock(Client.class);
		FeignLoadBalancer.RibbonRequest request = new FeignLoadBalancer.RibbonRequest(client, feignRequest, new URI("http://foo"));
		Response response = Response.builder().status(200).headers(new HashMap<String, Collection<String>>()).build();
		doReturn(response).when(client).execute(any(Request.class), any(Request.Options.class));
		RetryableFeignLoadBalancer feignLb = new RetryableFeignLoadBalancer(lb, config, inspector, loadBalancedRetryPolicyFactory);
		FeignLoadBalancer.RibbonResponse ribbonResponse = feignLb.execute(request, null);
		assertEquals(200, ribbonResponse.toResponse().status());
		verify(client, times(1)).execute(any(Request.class), any(Request.Options.class));
	}

	@Test
	public void executeNeverRetry() throws Exception {
		HttpRequest springRequest = mock(HttpRequest.class);
		Request feignRequest = Request.create("GET", "http://foo", new HashMap<String, Collection<String>>(),
				new byte[]{}, StandardCharsets.UTF_8);
		Client client = mock(Client.class);
		FeignLoadBalancer.RibbonRequest request = new FeignLoadBalancer.RibbonRequest(client, feignRequest, new URI("http://foo"));
		doThrow(new IOException("boom")).when(client).execute(any(Request.class), any(Request.Options.class));
		RetryableFeignLoadBalancer feignLb = new RetryableFeignLoadBalancer(lb, config, inspector, new LoadBalancedRetryPolicyFactory() {
			@Override
			public LoadBalancedRetryPolicy create(String s, ServiceInstanceChooser serviceInstanceChooser) {
				return null;
			}
		});
		try {
			feignLb.execute(request, null);
		} catch(Exception e) {
			assertThat(e, instanceOf(IOException.class));
		} finally {
			verify(client, times(1)).execute(any(Request.class), any(Request.Options.class));
		}
	}

	@Test
	public void executeRetry() throws Exception {
		RibbonLoadBalancerContext lbContext = new RibbonLoadBalancerContext(lb, config);
		SpringClientFactory clientFactory = mock(SpringClientFactory.class);
		IClientConfig config = mock(IClientConfig.class);
		doReturn(1).when(config).get(eq(CommonClientConfigKey.MaxAutoRetries), anyInt());
		doReturn(1).when(config).get(eq(CommonClientConfigKey.MaxAutoRetriesNextServer), anyInt());
		doReturn(true).when(config).get(eq(CommonClientConfigKey.OkToRetryOnAllOperations), eq(false));
		doReturn(defaultConnectTimeout).when(config).get(eq(CommonClientConfigKey.ConnectTimeout));
		doReturn(defaultReadTimeout).when(config).get(eq(CommonClientConfigKey.ReadTimeout));
		doReturn("").when(config).getPropertyAsString(eq(RibbonLoadBalancedRetryPolicy.RETRYABLE_STATUS_CODES),eq(""));
		doReturn(config).when(clientFactory).getClientConfig(eq("default"));
		doReturn(lbContext).when(clientFactory).getLoadBalancerContext(any(String.class));
		RibbonLoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory = new RibbonLoadBalancedRetryPolicyFactory(clientFactory);
		HttpRequest springRequest = mock(HttpRequest.class);
		Request feignRequest = Request.create("GET", "http://foo", new HashMap<String, Collection<String>>(),
				new byte[]{}, StandardCharsets.UTF_8);
		Client client = mock(Client.class);
		FeignLoadBalancer.RibbonRequest request = new FeignLoadBalancer.RibbonRequest(client, feignRequest, new URI("http://foo"));
		Response response = Response.builder().status(200).headers(new HashMap<String, Collection<String>>()).build();
		doThrow(new IOException("boom")).doReturn(response).when(client).execute(any(Request.class), any(Request.Options.class));
		RetryableFeignLoadBalancer feignLb = new RetryableFeignLoadBalancer(lb, config, inspector, loadBalancedRetryPolicyFactory);
		FeignLoadBalancer.RibbonResponse ribbonResponse = feignLb.execute(request, null);
		assertEquals(200, ribbonResponse.toResponse().status());
		verify(client, times(2)).execute(any(Request.class), any(Request.Options.class));
	}

	@Test
	public void executeRetryOnStatusCode() throws Exception {
		RibbonLoadBalancerContext lbContext = new RibbonLoadBalancerContext(lb, config);
		SpringClientFactory clientFactory = mock(SpringClientFactory.class);
		IClientConfig config = mock(IClientConfig.class);
		doReturn(1).when(config).get(eq(CommonClientConfigKey.MaxAutoRetries), anyInt());
		doReturn(1).when(config).get(eq(CommonClientConfigKey.MaxAutoRetriesNextServer), anyInt());
		doReturn(true).when(config).get(eq(CommonClientConfigKey.OkToRetryOnAllOperations), eq(false));
		doReturn(defaultConnectTimeout).when(config).get(eq(CommonClientConfigKey.ConnectTimeout));
		doReturn(defaultReadTimeout).when(config).get(eq(CommonClientConfigKey.ReadTimeout));
		doReturn("404").when(config).getPropertyAsString(eq(RibbonLoadBalancedRetryPolicy.RETRYABLE_STATUS_CODES),eq(""));
		doReturn(config).when(clientFactory).getClientConfig(eq("default"));
		doReturn(lbContext).when(clientFactory).getLoadBalancerContext(any(String.class));
		RibbonLoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory = new RibbonLoadBalancedRetryPolicyFactory(clientFactory);
		HttpRequest springRequest = mock(HttpRequest.class);
		Request feignRequest = Request.create("GET", "http://foo", new HashMap<String, Collection<String>>(),
				new byte[]{}, StandardCharsets.UTF_8);
		Client client = mock(Client.class);
		FeignLoadBalancer.RibbonRequest request = new FeignLoadBalancer.RibbonRequest(client, feignRequest, new URI("http://foo"));
		Response response = Response.builder().status(200).headers(new HashMap<String, Collection<String>>()).build();
		Response fourOFourResponse = Response.builder().status(404).headers(new HashMap<String, Collection<String>>()).build();
		doReturn(fourOFourResponse).doReturn(response).when(client).execute(any(Request.class), any(Request.Options.class));
		RetryableFeignLoadBalancer feignLb = new RetryableFeignLoadBalancer(lb, config, inspector, loadBalancedRetryPolicyFactory);
		FeignLoadBalancer.RibbonResponse ribbonResponse = feignLb.execute(request, null);
		assertEquals(200, ribbonResponse.toResponse().status());
		verify(client, times(2)).execute(any(Request.class), any(Request.Options.class));
	}

	@Test
	public void getRequestSpecificRetryHandler() throws Exception {
		RibbonLoadBalancerContext lbContext = new RibbonLoadBalancerContext(lb, config);
		SpringClientFactory clientFactory = mock(SpringClientFactory.class);
		doReturn(lbContext).when(clientFactory).getLoadBalancerContext(any(String.class));
		RibbonLoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory = new RibbonLoadBalancedRetryPolicyFactory(clientFactory);
		HttpRequest springRequest = mock(HttpRequest.class);
		Request feignRequest = Request.create("GET", "http://foo", new HashMap<String, Collection<String>>(),
				new byte[]{}, StandardCharsets.UTF_8);
		Client client = mock(Client.class);
		FeignLoadBalancer.RibbonRequest request = new FeignLoadBalancer.RibbonRequest(client, feignRequest, new URI("http://foo"));
		Response response = Response.builder().status(200).headers(new HashMap<String, Collection<String>>()).build();
		doReturn(response).when(client).execute(any(Request.class), any(Request.Options.class));
		RetryableFeignLoadBalancer feignLb = new RetryableFeignLoadBalancer(lb, config, inspector, loadBalancedRetryPolicyFactory);
		RequestSpecificRetryHandler retryHandler = feignLb.getRequestSpecificRetryHandler(request, config);
		assertEquals(1, retryHandler.getMaxRetriesOnNextServer());
		assertEquals(1, retryHandler.getMaxRetriesOnSameServer());

	}

	@Test
	public void choose() throws Exception {
		RibbonLoadBalancerContext lbContext = new RibbonLoadBalancerContext(lb, config);
		SpringClientFactory clientFactory = mock(SpringClientFactory.class);
		doReturn(lbContext).when(clientFactory).getLoadBalancerContext(any(String.class));
		RibbonLoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory = new RibbonLoadBalancedRetryPolicyFactory(clientFactory);
		HttpRequest springRequest = mock(HttpRequest.class);
		Request feignRequest = Request.create("GET", "http://foo", new HashMap<String, Collection<String>>(),
				new byte[]{}, StandardCharsets.UTF_8);
		Client client = mock(Client.class);
		FeignLoadBalancer.RibbonRequest request = new FeignLoadBalancer.RibbonRequest(client, feignRequest, new URI("http://foo"));
		Response response = Response.builder().status(200).headers(new HashMap<String, Collection<String>>()).build();
		doReturn(response).when(client).execute(any(Request.class), any(Request.Options.class));
		final Server server = new Server("foo", 80);
		RetryableFeignLoadBalancer feignLb = new RetryableFeignLoadBalancer(new ILoadBalancer() {
			@Override
			public void addServers(List<Server> list) {

			}

			@Override
			public Server chooseServer(Object o) {
				return server;
			}

			@Override
			public void markServerDown(Server server) {

			}

			@Override
			public List<Server> getServerList(boolean b) {
				return null;
			}

			@Override
			public List<Server> getReachableServers() {
				return null;
			}

			@Override
			public List<Server> getAllServers() {
				return null;
			}
		}, config, inspector, loadBalancedRetryPolicyFactory);
		ServiceInstance serviceInstance = feignLb.choose("foo");
		assertEquals("foo", serviceInstance.getHost());
		assertEquals(80, serviceInstance.getPort());

	}

}