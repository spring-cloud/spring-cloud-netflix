/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.cloud.netflix.ribbon.apache;

import java.io.IOException;
import java.net.URI;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicyFactory;
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancedRetryPolicy;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancedRetryPolicyFactory;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerContext;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;

import com.netflix.client.DefaultLoadBalancerRetryHandler;
import com.netflix.client.RetryHandler;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AbstractLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Sébastien Nussbaumer
 * @author Ryan Baxter
 */
public class RibbonLoadBalancingHttpClientTests {

	private ILoadBalancer loadBalancer;

	@Before
	public void setup() {
		loadBalancer = mock(AbstractLoadBalancer.class);
		doReturn(new Server("foo.com", 8000)).when(loadBalancer).chooseServer(eq("default"));
		doReturn(new Server("foo.com", 8000)).when(loadBalancer).chooseServer(eq("service"));
	}

	@After
	public void teardown() {
		loadBalancer = null;
	}

	@Test
	public void testRequestConfigUseDefaultsNoOverride() throws Exception {
		RequestConfig result = getBuiltRequestConfig(UseDefaults.class, null);

		assertThat(result.isRedirectsEnabled(), is(false));
	}

	@Test
	public void testRequestConfigDoNotFollowRedirectsNoOverride() throws Exception {
		RequestConfig result = getBuiltRequestConfig(DoNotFollowRedirects.class, null);

		assertThat(result.isRedirectsEnabled(), is(false));
	}

	@Test
	public void testRequestConfigFollowRedirectsNoOverride() throws Exception {
		RequestConfig result = getBuiltRequestConfig(FollowRedirects.class, null);

		assertThat(result.isRedirectsEnabled(), is(true));
	}

	@Test
	public void testTimeouts() throws Exception {
		RequestConfig result = getBuiltRequestConfig(Timeouts.class, null);
		assertThat(result.getConnectTimeout(), is(60000));
		assertThat(result.getSocketTimeout(), is (50000));
	}

	@Test
	public void testConnections() throws Exception {
		SpringClientFactory factory = new SpringClientFactory();
		factory.setApplicationContext(new AnnotationConfigApplicationContext(
				RibbonAutoConfiguration.class, Connections.class));
		RetryableRibbonLoadBalancingHttpClient client = factory.getClient("service",
				RetryableRibbonLoadBalancingHttpClient.class);

		HttpClient delegate = client.getDelegate();
		PoolingHttpClientConnectionManager connManager = (PoolingHttpClientConnectionManager) ReflectionTestUtils.getField(delegate, "connManager");
		assertThat(connManager.getMaxTotal(), is(101));
		assertThat(connManager.getDefaultMaxPerRoute(), is(201));
	}

	@Test
	public void testRequestConfigDoNotFollowRedirectsOverrideWithFollowRedirects()
			throws Exception {

		DefaultClientConfigImpl override = new DefaultClientConfigImpl();
		override.set(CommonClientConfigKey.FollowRedirects, true);
		override.set(CommonClientConfigKey.IsSecure, false);

		RequestConfig result = getBuiltRequestConfig(DoNotFollowRedirects.class, override);

		assertThat(result.isRedirectsEnabled(), is(true));
	}

	@Test
	public void testRequestConfigFollowRedirectsOverrideWithDoNotFollowRedirects()
			throws Exception {

		DefaultClientConfigImpl override = new DefaultClientConfigImpl();
		override.set(CommonClientConfigKey.FollowRedirects, false);
		override.set(CommonClientConfigKey.IsSecure, false);

		RequestConfig result = getBuiltRequestConfig(FollowRedirects.class, override);

		assertThat(result.isRedirectsEnabled(), is(false));
	}

	@Test
	public void testUpdatedTimeouts()
			throws Exception {
		SpringClientFactory factory = new SpringClientFactory();
		RequestConfig result = getBuiltRequestConfig(Timeouts.class, null, factory);
		assertThat(result.getConnectTimeout(), is(60000));
		assertThat(result.getSocketTimeout(), is (50000));
		IClientConfig config = factory.getClientConfig("service");
		config.set(CommonClientConfigKey.ConnectTimeout, 60);
		config.set(CommonClientConfigKey.ReadTimeout, 50);
		result = getBuiltRequestConfig(Timeouts.class, null, factory);
		assertThat(result.getConnectTimeout(), is(60));
		assertThat(result.getSocketTimeout(), is (50));
	}

	@Test
	public void testNeverRetry() throws Exception {
		ServerIntrospector introspector = mock(ServerIntrospector.class);
		HttpClient delegate = mock(HttpClient.class);
		HttpResponse response = mock(HttpResponse.class);
		doThrow(new IOException("boom")).when(delegate).execute(any(HttpUriRequest.class));
		DefaultClientConfigImpl clientConfig = new DefaultClientConfigImpl();
		clientConfig.setClientName("foo");
		RibbonLoadBalancingHttpClient client = new RibbonLoadBalancingHttpClient(delegate, clientConfig,
				introspector);
		RibbonApacheHttpRequest request = mock(RibbonApacheHttpRequest.class);
		try {
			client.execute(request, null);
			fail("Expected IOException");
		} catch(IOException e) {} finally {
			verify(delegate, times(1)).execute(any(HttpUriRequest.class));
		}
	}

	private RetryableRibbonLoadBalancingHttpClient setupClientForRetry(int retriesNextServer, int retriesSameServer,
															  boolean retryable, boolean retryOnAllOps,
															  String serviceName, String host, int port,
															  HttpClient delegate, ILoadBalancer lb, String statusCodes) throws Exception {
		ServerIntrospector introspector = mock(ServerIntrospector.class);
		RetryHandler retryHandler = new DefaultLoadBalancerRetryHandler(retriesSameServer, retriesNextServer, retryable);
		doReturn(new Server(host, port)).when(lb).chooseServer(eq(serviceName));
		DefaultClientConfigImpl clientConfig = new DefaultClientConfigImpl();
		clientConfig.set(CommonClientConfigKey.OkToRetryOnAllOperations, retryOnAllOps);
		clientConfig.set(CommonClientConfigKey.MaxAutoRetriesNextServer, retriesNextServer);
		clientConfig.set(CommonClientConfigKey.MaxAutoRetries, retriesSameServer);
		clientConfig.set(RibbonLoadBalancedRetryPolicy.RETRYABLE_STATUS_CODES, statusCodes);
		clientConfig.setClientName(serviceName);
		RibbonLoadBalancerContext context = new RibbonLoadBalancerContext(lb, clientConfig, retryHandler);
		SpringClientFactory clientFactory = mock(SpringClientFactory.class);
		doReturn(context).when(clientFactory).getLoadBalancerContext(eq(serviceName));
		doReturn(clientConfig).when(clientFactory).getClientConfig(eq(serviceName));
		LoadBalancedRetryPolicyFactory factory = new RibbonLoadBalancedRetryPolicyFactory(clientFactory);
		RetryableRibbonLoadBalancingHttpClient client = new RetryableRibbonLoadBalancingHttpClient(clientConfig, introspector, factory);
		client.setLoadBalancer(lb);
		ReflectionTestUtils.setField(client, "delegate", delegate);
		return client;
	}

	@Test
	public void testRetrySameServerOnly() throws Exception {
		int retriesNextServer = 0;
		int retriesSameServer = 1;
		boolean retryable = true;
		boolean retryOnAllOps = false;
		String serviceName = "foo";
		String host = serviceName;
		int port = 80;
		HttpMethod method = HttpMethod.GET;
		URI uri = new URI("http://" + host + ":" + port);
		HttpClient delegate = mock(HttpClient.class);
		final HttpResponse response = mock(HttpResponse.class);
		StatusLine statusLine = mock(StatusLine.class);
		doReturn(200).when(statusLine).getStatusCode();
		doReturn(statusLine).when(response).getStatusLine();
		doThrow(new IOException("boom")).doReturn(response).when(delegate).execute(any(HttpUriRequest.class));
		ILoadBalancer lb = mock(ILoadBalancer.class);
		RetryableRibbonLoadBalancingHttpClient client = setupClientForRetry(retriesNextServer, retriesSameServer, retryable, retryOnAllOps,
				serviceName, host, port, delegate, lb, "");
		RibbonApacheHttpRequest request = mock(RibbonApacheHttpRequest.class);
		doReturn(uri).when(request).getURI();
		doReturn(method).when(request).getMethod();
		doReturn(request).when(request).withNewUri(any(URI.class));
		HttpUriRequest uriRequest = mock(HttpUriRequest.class);
		doReturn(uri).when(uriRequest).getURI();
		doReturn(uriRequest).when(request).toRequest(any(RequestConfig.class));
		RibbonApacheHttpResponse returnedResponse = client.execute(request, null);
		verify(delegate, times(2)).execute(any(HttpUriRequest.class));
		verify(lb, times(0)).chooseServer(eq(serviceName));
	}

	@Test
	public void testRetryNextServer() throws Exception {
		int retriesNextServer = 1;
		int retriesSameServer = 1;
		boolean retryable = true;
		boolean retryOnAllOps = false;
		String serviceName = "foo";
		String host = serviceName;
		int port = 80;
		HttpMethod method = HttpMethod.GET;
		URI uri = new URI("http://" + host + ":" + port);
		HttpClient delegate = mock(HttpClient.class);
		final HttpResponse response = mock(HttpResponse.class);
		StatusLine statusLine = mock(StatusLine.class);
		doReturn(200).when(statusLine).getStatusCode();
		doReturn(statusLine).when(response).getStatusLine();
		doThrow(new IOException("boom")).doThrow(new IOException("boom again")).doReturn(response).
				when(delegate).execute(any(HttpUriRequest.class));
		ILoadBalancer lb = mock(ILoadBalancer.class);
		RetryableRibbonLoadBalancingHttpClient client = setupClientForRetry(retriesNextServer, retriesSameServer, retryable, retryOnAllOps,
				serviceName, host, port, delegate, lb, "");
		RibbonApacheHttpRequest request = mock(RibbonApacheHttpRequest.class);
		doReturn(uri).when(request).getURI();
		doReturn(method).when(request).getMethod();
		doReturn(request).when(request).withNewUri(any(URI.class));
		HttpUriRequest uriRequest = mock(HttpUriRequest.class);
		doReturn(uri).when(uriRequest).getURI();
		doReturn(uriRequest).when(request).toRequest(any(RequestConfig.class));
		RibbonApacheHttpResponse returnedResponse = client.execute(request, null);
		verify(delegate, times(3)).execute(any(HttpUriRequest.class));
		verify(lb, times(1)).chooseServer(eq(serviceName));
	}

	@Test
	public void testRetryOnPost() throws Exception {
		int retriesNextServer = 1;
		int retriesSameServer = 1;
		boolean retryable = true;
		boolean retryOnAllOps = true;
		String serviceName = "foo";
		String host = serviceName;
		int port = 80;
		HttpMethod method = HttpMethod.POST;
		URI uri = new URI("http://" + host + ":" + port);
		HttpClient delegate = mock(HttpClient.class);
		final HttpResponse response = mock(HttpResponse.class);
		StatusLine statusLine = mock(StatusLine.class);
		doReturn(200).when(statusLine).getStatusCode();
		doReturn(statusLine).when(response).getStatusLine();
		doThrow(new IOException("boom")).doThrow(new IOException("boom again")).doReturn(response).
				when(delegate).execute(any(HttpUriRequest.class));
		ILoadBalancer lb = mock(ILoadBalancer.class);
		RetryableRibbonLoadBalancingHttpClient client = setupClientForRetry(retriesNextServer, retriesSameServer, retryable, retryOnAllOps,
				serviceName, host, port, delegate, lb, "");
		RibbonApacheHttpRequest request = mock(RibbonApacheHttpRequest.class);
		doReturn(method).when(request).getMethod();
		doReturn(uri).when(request).getURI();
		doReturn(request).when(request).withNewUri(any(URI.class));
		HttpUriRequest uriRequest = mock(HttpUriRequest.class);
		doReturn(uriRequest).when(request).toRequest(any(RequestConfig.class));
		RibbonApacheHttpResponse returnedResponse = client.execute(request, null);
		verify(delegate, times(3)).execute(any(HttpUriRequest.class));
		verify(lb, times(1)).chooseServer(eq(serviceName));
	}

	@Test
	public void testNoRetryOnPost() throws Exception {
		int retriesNextServer = 1;
		int retriesSameServer = 1;
		boolean retryable = true;
		boolean retryOnAllOps = false;
		String serviceName = "foo";
		String host = serviceName;
		int port = 80;
		HttpMethod method = HttpMethod.POST;
		URI uri = new URI("http://" + host + ":" + port);
		HttpClient delegate = mock(HttpClient.class);
		final HttpResponse response = mock(HttpResponse.class);
		doThrow(new IOException("boom")).doThrow(new IOException("boom again")).doReturn(response).
				when(delegate).execute(any(HttpUriRequest.class));
		ILoadBalancer lb = mock(ILoadBalancer.class);
		RetryableRibbonLoadBalancingHttpClient client = setupClientForRetry(retriesNextServer, retriesSameServer, retryable, retryOnAllOps,
				serviceName, host, port, delegate, lb, "");
		RibbonApacheHttpRequest request = mock(RibbonApacheHttpRequest.class);
		doReturn(method).when(request).getMethod();
		doReturn(uri).when(request).getURI();
		doReturn(request).when(request).withNewUri(any(URI.class));
		HttpUriRequest uriRequest = mock(HttpUriRequest.class);
		doReturn(uri).when(uriRequest).getURI();
		doReturn(uriRequest).when(request).toRequest(any(RequestConfig.class));
		try {
			client.execute(request, null);
			fail("Expected IOException");
		} catch(IOException e) {} finally {
			verify(delegate, times(1)).execute(any(HttpUriRequest.class));
			verify(lb, times(0)).chooseServer(eq(serviceName));
		}
	}

	@Test
	public void testRetryOnStatusCode() throws Exception {
		int retriesNextServer = 0;
		int retriesSameServer = 1;
		boolean retryable = true;
		boolean retryOnAllOps = false;
		String serviceName = "foo";
		String host = serviceName;
		int port = 80;
		HttpMethod method = HttpMethod.GET;
		URI uri = new URI("http://" + host + ":" + port);
		HttpClient delegate = mock(HttpClient.class);
		final HttpResponse response = mock(HttpResponse.class);
		StatusLine statusLine = mock(StatusLine.class);
		doReturn(200).when(statusLine).getStatusCode();
		doReturn(statusLine).when(response).getStatusLine();
		final HttpResponse fourOFourResponse = mock(HttpResponse.class);
		StatusLine fourOFourStatusLine = mock(StatusLine.class);
		doReturn(404).when(fourOFourStatusLine).getStatusCode();
		doReturn(fourOFourStatusLine).when(fourOFourResponse).getStatusLine();
		doReturn(fourOFourResponse).doReturn(response).when(delegate).execute(any(HttpUriRequest.class));
		ILoadBalancer lb = mock(ILoadBalancer.class);
		RetryableRibbonLoadBalancingHttpClient client = setupClientForRetry(retriesNextServer, retriesSameServer, retryable, retryOnAllOps,
				serviceName, host, port, delegate, lb, "404");
		RibbonApacheHttpRequest request = mock(RibbonApacheHttpRequest.class);
		doReturn(uri).when(request).getURI();
		doReturn(method).when(request).getMethod();
		doReturn(request).when(request).withNewUri(any(URI.class));
		HttpUriRequest uriRequest = mock(HttpUriRequest.class);
		doReturn(uri).when(uriRequest).getURI();
		doReturn(uriRequest).when(request).toRequest(any(RequestConfig.class));
		RibbonApacheHttpResponse returnedResponse = client.execute(request, null);
		verify(delegate, times(2)).execute(any(HttpUriRequest.class));
		verify(lb, times(0)).chooseServer(eq(serviceName));
	}

	@Configuration
	protected static class UseDefaults {

	}

	@Configuration
	protected static class FollowRedirects {
		@Bean
		public IClientConfig clientConfig() {
			DefaultClientConfigImpl config = new DefaultClientConfigImpl();
			config.set(CommonClientConfigKey.FollowRedirects, true);
			return config;
		}
	}

	@Configuration
	protected static class DoNotFollowRedirects {
		@Bean
		public IClientConfig clientConfig() {
			DefaultClientConfigImpl config = new DefaultClientConfigImpl();
			config.set(CommonClientConfigKey.FollowRedirects, false);
			return config;
		}
	}

	@Configuration
	protected static class Timeouts {
		@Bean
		public IClientConfig clientConfig() {
			DefaultClientConfigImpl config = new DefaultClientConfigImpl();
			config.set(CommonClientConfigKey.ConnectTimeout, 60000);
			config.set(CommonClientConfigKey.ReadTimeout, 50000);
			return config;
		}
	}


	@Configuration
	protected static class Connections {
		@Bean
		public IClientConfig clientConfig() {
			DefaultClientConfigImpl config = new DefaultClientConfigImpl();
			config.set(CommonClientConfigKey.MaxTotalConnections, 101);
			config.set(CommonClientConfigKey.MaxConnectionsPerHost, 201);
			return config;
		}
	}

	private RequestConfig getBuiltRequestConfig(Class<?> defaultConfigurationClass,
			IClientConfig configOverride) throws Exception {
		return getBuiltRequestConfig(defaultConfigurationClass, configOverride, new SpringClientFactory());
	}

	private RequestConfig getBuiltRequestConfig(Class<?> defaultConfigurationClass,
												IClientConfig configOverride, SpringClientFactory factory)
			throws Exception {

		factory.setApplicationContext(new AnnotationConfigApplicationContext(
				RibbonAutoConfiguration.class, defaultConfigurationClass));
		String serviceName = "foo";
		String host = serviceName;
		int port = 80;
		URI uri = new URI("http://" + host + ":" + port);
		HttpClient delegate = mock(HttpClient.class);
		RibbonLoadBalancingHttpClient client = factory.getClient("service",
				RibbonLoadBalancingHttpClient.class);

		ReflectionTestUtils.setField(client, "delegate", delegate);
		ReflectionTestUtils.setField(client, "lb", loadBalancer);
		HttpResponse httpResponse  = mock(HttpResponse.class);
		StatusLine statusLine = mock(StatusLine.class);
		doReturn(200).when(statusLine).getStatusCode();
		doReturn(statusLine).when(httpResponse).getStatusLine();
		given(delegate.execute(any(HttpUriRequest.class))).willReturn(
				httpResponse);
		RibbonApacheHttpRequest request = mock(RibbonApacheHttpRequest.class);
		doReturn(uri).when(request).getURI();
		doReturn(request).when(request).withNewUri(any(URI.class));
		given(request.toRequest(any(RequestConfig.class))).willReturn(
				mock(HttpUriRequest.class));

		client.execute(request, configOverride);

		ArgumentCaptor<RequestConfig> requestConfigCaptor = ArgumentCaptor
				.forClass(RequestConfig.class);
		verify(request, times(1)).toRequest(requestConfigCaptor.capture());
		return requestConfigCaptor.getValue();
	}

}
