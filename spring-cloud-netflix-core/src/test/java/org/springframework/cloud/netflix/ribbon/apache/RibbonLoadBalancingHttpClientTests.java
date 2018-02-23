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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Locale;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.cloud.client.loadbalancer.LoadBalancedBackOffPolicyFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryListenerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicyFactory;
import org.springframework.cloud.commons.httpclient.HttpClientConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancedRetryPolicy;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancedRetryPolicyFactory;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerContext;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.ribbon.support.RibbonRequestCustomizer;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.TerminatedRetryException;
import org.springframework.retry.backoff.BackOffContext;
import org.springframework.retry.backoff.BackOffInterruptedException;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;

import com.netflix.client.ClientException;
import com.netflix.client.DefaultLoadBalancerRetryHandler;
import com.netflix.client.RetryHandler;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AbstractLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Sébastien Nussbaumer
 * @author Ryan Baxter
 * @author Gang Li
 */
public class RibbonLoadBalancingHttpClientTests {

	private ILoadBalancer loadBalancer;
	private LoadBalancedBackOffPolicyFactory loadBalancedBackOffPolicyFactory = new LoadBalancedBackOffPolicyFactory.NoBackOffPolicyFactory();
	private LoadBalancedRetryListenerFactory loadBalancedRetryListenerFactory = new LoadBalancedRetryListenerFactory.DefaultRetryListenerFactory();

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
	public void testDefaultTimeouts() throws Exception {
		RequestConfig result = getBuiltRequestConfig(UseDefaults.class, null);
		assertThat(result.getConnectTimeout(), is(1000));
		assertThat(result.getSocketTimeout(), is (1000));
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
		CloseableHttpClient delegate = mock(CloseableHttpClient.class);
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

	@Test
	public void testRetryFail() throws Exception {
		int retriesNextServer = 0;
		int retriesSameServer = 1;
		boolean retryable = true;
		boolean retryOnAllOps = false;
		String serviceName = "foo";
		String host = serviceName;
		int port = 80;
		HttpMethod method = HttpMethod.GET;
		URI uri = new URI("http://" + host + ":" + port);
		CloseableHttpClient delegate = mock(CloseableHttpClient.class);
		StatusLine fourOFourStatusLine = mock(StatusLine.class);
		CloseableHttpResponse fourOFourResponse = mock(CloseableHttpResponse.class);
		Locale locale = new Locale("en");
		doReturn(locale).when(fourOFourResponse).getLocale();
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
				HttpEntity entity = mock(HttpEntity.class);
				doReturn(new ByteArrayInputStream("test".getBytes())).when(entity).getContent();
				return entity;
			}
		}).when(fourOFourResponse).getEntity();
		doReturn(404).when(fourOFourStatusLine).getStatusCode();
		doReturn(fourOFourStatusLine).when(fourOFourResponse).getStatusLine();
		doReturn(locale).when(fourOFourResponse).getLocale();
		doReturn(fourOFourResponse).when(delegate).execute(any(HttpUriRequest.class));
		ILoadBalancer lb = mock(ILoadBalancer.class);
		MyBackOffPolicyFactory myBackOffPolicyFactory = new MyBackOffPolicyFactory();
		RetryableRibbonLoadBalancingHttpClient client = setupClientForRetry(retriesNextServer, retriesSameServer, retryable, retryOnAllOps,
				serviceName, host, port, delegate, lb, "404", myBackOffPolicyFactory);
		RibbonApacheHttpRequest request = mock(RibbonApacheHttpRequest.class);
		doReturn(uri).when(request).getURI();
		doReturn(method).when(request).getMethod();
		doReturn(request).when(request).withNewUri(any(URI.class));
		HttpUriRequest uriRequest = mock(HttpUriRequest.class);
		doReturn(uri).when(uriRequest).getURI();
		doReturn(uriRequest).when(request).toRequest(any(RequestConfig.class));
		RibbonApacheHttpResponse returnedResponse = client.execute(request, null);
		verify(delegate, times(2)).execute(any(HttpUriRequest.class));
		byte[] buf = new byte[100];
		InputStream inputStream = returnedResponse.getInputStream();
		int read = inputStream.read(buf);
		assertThat(new String(buf, 0, read), is("test"));
	}

	private RetryableRibbonLoadBalancingHttpClient setupClientForRetry(int retriesNextServer, int retriesSameServer,
																	   boolean retryable, boolean retryOnAllOps,
																	   String serviceName, String host, int port,
																	   CloseableHttpClient delegate, ILoadBalancer lb, String statusCodes,
																	   LoadBalancedBackOffPolicyFactory loadBalancedBackOffPolicyFactory) throws Exception {
		return setupClientForRetry(retriesNextServer, retriesSameServer, retryable, retryOnAllOps, serviceName, host, port,
				delegate, lb, statusCodes, loadBalancedBackOffPolicyFactory, false);
	}

	private RetryableRibbonLoadBalancingHttpClient setupClientForRetry(int retriesNextServer, int retriesSameServer,
																	   boolean retryable, boolean retryOnAllOps,
																	   String serviceName, String host, int port,
																	   CloseableHttpClient delegate, ILoadBalancer lb, String statusCodes,
																	   LoadBalancedBackOffPolicyFactory loadBalancedBackOffPolicyFactory,
																	   boolean isSecure) throws Exception {
		ServerIntrospector introspector = mock(ServerIntrospector.class);
		RetryHandler retryHandler = new DefaultLoadBalancerRetryHandler(retriesSameServer, retriesNextServer, retryable);
		doReturn(new Server(host, port)).when(lb).chooseServer(eq(serviceName));
		DefaultClientConfigImpl clientConfig = new DefaultClientConfigImpl();
		clientConfig.set(CommonClientConfigKey.OkToRetryOnAllOperations, retryOnAllOps);
		clientConfig.set(CommonClientConfigKey.MaxAutoRetriesNextServer, retriesNextServer);
		clientConfig.set(CommonClientConfigKey.MaxAutoRetries, retriesSameServer);
		clientConfig.set(RibbonLoadBalancedRetryPolicy.RETRYABLE_STATUS_CODES, statusCodes);
		clientConfig.set(CommonClientConfigKey.IsSecure, isSecure);
		clientConfig.setClientName(serviceName);
		RibbonLoadBalancerContext context = new RibbonLoadBalancerContext(lb, clientConfig, retryHandler);
		SpringClientFactory clientFactory = mock(SpringClientFactory.class);
		doReturn(context).when(clientFactory).getLoadBalancerContext(eq(serviceName));
		doReturn(clientConfig).when(clientFactory).getClientConfig(eq(serviceName));
		LoadBalancedRetryPolicyFactory factory = new RibbonLoadBalancedRetryPolicyFactory(clientFactory);
		RetryableRibbonLoadBalancingHttpClient client = new RetryableRibbonLoadBalancingHttpClient(delegate, clientConfig,
				introspector, factory, loadBalancedBackOffPolicyFactory, new LoadBalancedRetryListenerFactory.DefaultRetryListenerFactory());
		client.setLoadBalancer(lb);
		ReflectionTestUtils.setField(client, "delegate", delegate);
		return client;
	}

	private RetryableRibbonLoadBalancingHttpClient setupClientForRetry(int retriesNextServer, int retriesSameServer,
																	   boolean retryable, boolean retryOnAllOps,
																	   String serviceName, String host, int port,
																	   CloseableHttpClient delegate, ILoadBalancer lb, String statusCodes,
																	   LoadBalancedBackOffPolicyFactory loadBalancedBackOffPolicyFactory,
																	   boolean isSecure, LoadBalancedRetryListenerFactory loadBalancedRetryListenerFactory) throws Exception {
		ServerIntrospector introspector = mock(ServerIntrospector.class);
		RetryHandler retryHandler = new DefaultLoadBalancerRetryHandler(retriesSameServer, retriesNextServer, retryable);
		doReturn(new Server(host, port)).when(lb).chooseServer(eq(serviceName));
		DefaultClientConfigImpl clientConfig = new DefaultClientConfigImpl();
		clientConfig.set(CommonClientConfigKey.OkToRetryOnAllOperations, retryOnAllOps);
		clientConfig.set(CommonClientConfigKey.MaxAutoRetriesNextServer, retriesNextServer);
		clientConfig.set(CommonClientConfigKey.MaxAutoRetries, retriesSameServer);
		clientConfig.set(RibbonLoadBalancedRetryPolicy.RETRYABLE_STATUS_CODES, statusCodes);
		clientConfig.set(CommonClientConfigKey.IsSecure, isSecure);
		clientConfig.setClientName(serviceName);
		RibbonLoadBalancerContext context = new RibbonLoadBalancerContext(lb, clientConfig, retryHandler);
		SpringClientFactory clientFactory = mock(SpringClientFactory.class);
		doReturn(context).when(clientFactory).getLoadBalancerContext(eq(serviceName));
		doReturn(clientConfig).when(clientFactory).getClientConfig(eq(serviceName));
		LoadBalancedRetryPolicyFactory factory = new RibbonLoadBalancedRetryPolicyFactory(clientFactory);
		RetryableRibbonLoadBalancingHttpClient client = new RetryableRibbonLoadBalancingHttpClient(delegate, clientConfig,
			introspector, factory, loadBalancedBackOffPolicyFactory, loadBalancedRetryListenerFactory);
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
		CloseableHttpClient delegate = mock(CloseableHttpClient.class);
		final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		StatusLine statusLine = mock(StatusLine.class);
		doReturn(200).when(statusLine).getStatusCode();
		doReturn(statusLine).when(response).getStatusLine();
		doThrow(new IOException("boom")).doReturn(response).when(delegate).execute(any(HttpUriRequest.class));
		ILoadBalancer lb = mock(ILoadBalancer.class);
		RetryableRibbonLoadBalancingHttpClient client = setupClientForRetry(retriesNextServer, retriesSameServer, retryable, retryOnAllOps,
				serviceName, host, port, delegate, lb, "", loadBalancedBackOffPolicyFactory);
		RibbonApacheHttpRequest request = mock(RibbonApacheHttpRequest.class);
		doReturn(uri).when(request).getURI();
		doReturn(method).when(request).getMethod();
		doReturn(request).when(request).withNewUri(any(URI.class));
		HttpUriRequest uriRequest = mock(HttpUriRequest.class);
		doReturn(uri).when(uriRequest).getURI();
		doReturn(uriRequest).when(request).toRequest(any(RequestConfig.class));
		RibbonApacheHttpResponse returnedResponse = client.execute(request, null);
		verify(delegate, times(2)).execute(any(HttpUriRequest.class));
		verify(lb, times(1)).chooseServer(eq(serviceName));
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
		CloseableHttpClient delegate = mock(CloseableHttpClient.class);
		final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		StatusLine statusLine = mock(StatusLine.class);
		doReturn(200).when(statusLine).getStatusCode();
		doReturn(statusLine).when(response).getStatusLine();
		doThrow(new IOException("boom")).doThrow(new IOException("boom again")).doReturn(response).
				when(delegate).execute(any(HttpUriRequest.class));
		ILoadBalancer lb = mock(ILoadBalancer.class);
		MyBackOffPolicyFactory myBackOffPolicyFactory = new MyBackOffPolicyFactory();
		RetryableRibbonLoadBalancingHttpClient client = setupClientForRetry(retriesNextServer, retriesSameServer, retryable, retryOnAllOps,
				serviceName, host, port, delegate, lb, "", myBackOffPolicyFactory);
		RibbonApacheHttpRequest request = mock(RibbonApacheHttpRequest.class);
		doReturn(uri).when(request).getURI();
		doReturn(method).when(request).getMethod();
		doReturn(request).when(request).withNewUri(any(URI.class));
		HttpUriRequest uriRequest = mock(HttpUriRequest.class);
		doReturn(uri).when(uriRequest).getURI();
		doReturn(uriRequest).when(request).toRequest(any(RequestConfig.class));
		client.execute(request, null);
		verify(delegate, times(3)).execute(any(HttpUriRequest.class));
		verify(lb, times(2)).chooseServer(eq(serviceName));
		assertEquals(2, myBackOffPolicyFactory.getCount());
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
		CloseableHttpClient delegate = mock(CloseableHttpClient.class);
		final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		StatusLine statusLine = mock(StatusLine.class);
		doReturn(200).when(statusLine).getStatusCode();
		doReturn(statusLine).when(response).getStatusLine();
		doThrow(new IOException("boom")).doThrow(new IOException("boom again")).doReturn(response).
				when(delegate).execute(any(HttpUriRequest.class));
		ILoadBalancer lb = mock(ILoadBalancer.class);
		MyBackOffPolicyFactory myBackOffPolicyFactory = new MyBackOffPolicyFactory();
		RetryableRibbonLoadBalancingHttpClient client = setupClientForRetry(retriesNextServer, retriesSameServer, retryable, retryOnAllOps,
				serviceName, host, port, delegate, lb, "", myBackOffPolicyFactory);
		RibbonApacheHttpRequest request = mock(RibbonApacheHttpRequest.class);
		doReturn(method).when(request).getMethod();
		doReturn(uri).when(request).getURI();
		doReturn(request).when(request).withNewUri(any(URI.class));
		HttpUriRequest uriRequest = mock(HttpUriRequest.class);
		doReturn(uriRequest).when(request).toRequest(any(RequestConfig.class));
		RibbonApacheHttpResponse returnedResponse = client.execute(request, null);
		verify(response, times(0)).close();
		verify(delegate, times(3)).execute(any(HttpUriRequest.class));
		verify(lb, times(2)).chooseServer(eq(serviceName));
		assertEquals(2, myBackOffPolicyFactory.getCount());
	}

	@Test
	public void testDoubleEncoding() throws Exception {
		String serviceName = "foo";
		String host = serviceName;
		int port = 80;
		HttpMethod method = HttpMethod.GET;
		final URI uri = new URI("https://" + host + ":" + port + "/a%2Bb");
		DefaultClientConfigImpl clientConfig = new DefaultClientConfigImpl();
		clientConfig.setClientName(serviceName);
		ServerIntrospector introspector = mock(ServerIntrospector.class);
		RibbonCommandContext context = new RibbonCommandContext(serviceName, method.toString(), uri.toString(), false,
				new LinkedMultiValueMap<String, String>(), new LinkedMultiValueMap<String, String>(),
				new ByteArrayInputStream("bar".getBytes()),
				new ArrayList<RibbonRequestCustomizer>());
		RibbonApacheHttpRequest request = new RibbonApacheHttpRequest(context);
		CloseableHttpClient delegate = mock(CloseableHttpClient.class);
		final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		StatusLine statusLine = mock(StatusLine.class);
		doReturn(200).when(statusLine).getStatusCode();
		doReturn(statusLine).when(response).getStatusLine();
		doReturn(response).
				when(delegate).execute(any(HttpUriRequest.class));
		RibbonLoadBalancingHttpClient client = new RibbonLoadBalancingHttpClient(delegate, clientConfig, introspector);
		client.execute(request, null);
		verify(response, times(0)).close();
		verify(delegate, times(1)).execute(argThat(new ArgumentMatcher<HttpUriRequest>() {
			@Override
			public boolean matches(Object argument) {
				if(argument instanceof HttpUriRequest) {
					HttpUriRequest arg = (HttpUriRequest)argument;
					return arg.getURI().equals(uri);
				}
				return false;
			}
		}));
	}

	@Test
	public void testDoubleEncodingWithRetry() throws Exception {
		int retriesNextServer = 0;
		int retriesSameServer = 0;
		boolean retryable = true;
		boolean retryOnAllOps = true;
		String serviceName = "foo";
		String host = serviceName;
		int port = 80;
		HttpMethod method = HttpMethod.GET;
		final URI uri = new URI("https://" + host + ":" + port + "/a%20b");
		RibbonCommandContext context = new RibbonCommandContext(serviceName, method.toString(), uri.toString(), true,
				new LinkedMultiValueMap<String, String>(), new LinkedMultiValueMap<String, String>(),
				new ByteArrayInputStream(new String("bar").getBytes()),
				new ArrayList<RibbonRequestCustomizer>());
		RibbonApacheHttpRequest request = new RibbonApacheHttpRequest(context);
		CloseableHttpClient delegate = mock(CloseableHttpClient.class);
		final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		StatusLine statusLine = mock(StatusLine.class);
		doReturn(200).when(statusLine).getStatusCode();
		doReturn(statusLine).when(response).getStatusLine();
		doReturn(response).
				when(delegate).execute(any(HttpUriRequest.class));
		ILoadBalancer lb = mock(ILoadBalancer.class);
		RetryableRibbonLoadBalancingHttpClient client = setupClientForRetry(retriesNextServer, retriesSameServer, retryable, retryOnAllOps,
				serviceName, host, port, delegate, lb, "", loadBalancedBackOffPolicyFactory,true);
		client.execute(request, null);
		verify(response, times(0)).close();
		verify(delegate, times(1)).execute(argThat(new ArgumentMatcher<HttpUriRequest>() {
			@Override
			public boolean matches(Object argument) {
				if(argument instanceof HttpUriRequest) {
					HttpUriRequest arg = (HttpUriRequest)argument;
					return arg.getURI().equals(uri);
				}
				return false;
			}
		}));
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
		CloseableHttpClient delegate = mock(CloseableHttpClient.class);
		final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		doThrow(new IOException("boom")).doThrow(new IOException("boom again")).doReturn(response).
				when(delegate).execute(any(HttpUriRequest.class));
		ILoadBalancer lb = mock(ILoadBalancer.class);
		RetryableRibbonLoadBalancingHttpClient client = setupClientForRetry(retriesNextServer, retriesSameServer, retryable, retryOnAllOps,
				serviceName, host, port, delegate, lb, "", loadBalancedBackOffPolicyFactory);
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
			verify(response, times(0)).close();
			verify(delegate, times(1)).execute(any(HttpUriRequest.class));
			verify(lb, times(1)).chooseServer(eq(serviceName));
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
		CloseableHttpClient delegate = mock(CloseableHttpClient.class);
		final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		Locale locale = new Locale("en");
		doReturn(locale).when(response).getLocale();
		StatusLine statusLine = mock(StatusLine.class);
		doReturn(200).when(statusLine).getStatusCode();
		doReturn(statusLine).when(response).getStatusLine();
		final CloseableHttpResponse fourOFourResponse = mock(CloseableHttpResponse.class);
		doReturn(locale).when(fourOFourResponse).getLocale();
		BasicHttpEntity entity = new BasicHttpEntity();
		entity.setContentLength(5);
		entity.setContent(new ByteArrayInputStream("error".getBytes()));
		doReturn(entity).when(fourOFourResponse).getEntity();
		StatusLine fourOFourStatusLine = mock(StatusLine.class);
		doReturn(404).when(fourOFourStatusLine).getStatusCode();
		doReturn(fourOFourStatusLine).when(fourOFourResponse).getStatusLine();
		doReturn(fourOFourResponse).doReturn(response).when(delegate).execute(any(HttpUriRequest.class));
		ILoadBalancer lb = mock(ILoadBalancer.class);
		MyBackOffPolicyFactory myBackOffPolicyFactory = new MyBackOffPolicyFactory();
		RetryableRibbonLoadBalancingHttpClient client = setupClientForRetry(retriesNextServer, retriesSameServer, retryable, retryOnAllOps,
				serviceName, host, port, delegate, lb, "404", myBackOffPolicyFactory);
		RibbonApacheHttpRequest request = mock(RibbonApacheHttpRequest.class);
		doReturn(uri).when(request).getURI();
		doReturn(method).when(request).getMethod();
		doReturn(request).when(request).withNewUri(any(URI.class));
		HttpUriRequest uriRequest = mock(HttpUriRequest.class);
		doReturn(uri).when(uriRequest).getURI();
		doReturn(uriRequest).when(request).toRequest(any(RequestConfig.class));
		client.execute(request, null);
		verify(fourOFourResponse, times(1)).close();
		verify(delegate, times(2)).execute(any(HttpUriRequest.class));
		verify(lb, times(1)).chooseServer(eq(serviceName));
		assertEquals(1, myBackOffPolicyFactory.getCount());
	}

	@Test
	public void retryListenerTest() throws Exception {
		int retriesNextServer = 1;
		int retriesSameServer = 1;
		boolean retryable = true;
		boolean retryOnAllOps = true;
		String serviceName = "listener";
		String host = serviceName;
		int port = 80;
		HttpMethod method = HttpMethod.POST;
		URI uri = new URI("http://" + host + ":" + port);
		CloseableHttpClient delegate = mock(CloseableHttpClient.class);
		final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		StatusLine statusLine = mock(StatusLine.class);
		doReturn(200).when(statusLine).getStatusCode();
		doReturn(statusLine).when(response).getStatusLine();
		doThrow(new IOException("boom")).doThrow(new IOException("boom again")).doReturn(response).
			when(delegate).execute(any(HttpUriRequest.class));
		ILoadBalancer lb = mock(ILoadBalancer.class);
		MyBackOffPolicyFactory myBackOffPolicyFactory = new MyBackOffPolicyFactory();
		MyRetryListeners myRetryListeners = new MyRetryListeners();
		RetryableRibbonLoadBalancingHttpClient client = setupClientForRetry(retriesNextServer, retriesSameServer, retryable, retryOnAllOps,
			serviceName, host, port, delegate, lb, "", myBackOffPolicyFactory, false, myRetryListeners);
		RibbonApacheHttpRequest request = mock(RibbonApacheHttpRequest.class);
		doReturn(method).when(request).getMethod();
		doReturn(uri).when(request).getURI();
		doReturn(request).when(request).withNewUri(any(URI.class));
		HttpUriRequest uriRequest = mock(HttpUriRequest.class);
		doReturn(uriRequest).when(request).toRequest(any(RequestConfig.class));
		RibbonApacheHttpResponse returnedResponse = client.execute(request, null);
		verify(response, times(0)).close();
		verify(delegate, times(3)).execute(any(HttpUriRequest.class));
		verify(lb, times(2)).chooseServer(eq(serviceName));
		assertEquals(2, myBackOffPolicyFactory.getCount());
		assertEquals(2, myRetryListeners.getOnError());
	}

	@Test
	public void retryDefaultListenerTest() throws Exception {
		int retriesNextServer = 1;
		int retriesSameServer = 1;
		boolean retryable = true;
		boolean retryOnAllOps = true;
		String serviceName = "listener";
		String host = serviceName;
		int port = 80;
		HttpMethod method = HttpMethod.POST;
		URI uri = new URI("http://" + host + ":" + port);
		CloseableHttpClient delegate = mock(CloseableHttpClient.class);
		final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		StatusLine statusLine = mock(StatusLine.class);
		doReturn(200).when(statusLine).getStatusCode();
		doReturn(statusLine).when(response).getStatusLine();
		doThrow(new IOException("boom")).doThrow(new IOException("boom again")).doReturn(response).
			when(delegate).execute(any(HttpUriRequest.class));
		ILoadBalancer lb = mock(ILoadBalancer.class);
		MyBackOffPolicyFactory myBackOffPolicyFactory = new MyBackOffPolicyFactory();
		MyRetryListeners myRetryListeners = new MyRetryListeners();
		RetryableRibbonLoadBalancingHttpClient client = setupClientForRetry(retriesNextServer, retriesSameServer, retryable, retryOnAllOps,
			serviceName, host, port, delegate, lb, "", myBackOffPolicyFactory, false, loadBalancedRetryListenerFactory);
		RibbonApacheHttpRequest request = mock(RibbonApacheHttpRequest.class);
		doReturn(method).when(request).getMethod();
		doReturn(uri).when(request).getURI();
		doReturn(request).when(request).withNewUri(any(URI.class));
		HttpUriRequest uriRequest = mock(HttpUriRequest.class);
		doReturn(uriRequest).when(request).toRequest(any(RequestConfig.class));
		RibbonApacheHttpResponse returnedResponse = client.execute(request, null);
		verify(response, times(0)).close();
		verify(delegate, times(3)).execute(any(HttpUriRequest.class));
		verify(lb, times(2)).chooseServer(eq(serviceName));
		assertEquals(2, myBackOffPolicyFactory.getCount());
		assertEquals(0, myRetryListeners.getOnError());
	}

	@Test(expected = TerminatedRetryException.class)
	public void retryListenerTestNoRetry() throws Exception {
		int retriesNextServer = 1;
		int retriesSameServer = 1;
		boolean retryable = true;
		boolean retryOnAllOps = true;
		String serviceName = "listener";
		String host = serviceName;
		int port = 80;
		HttpMethod method = HttpMethod.POST;
		URI uri = new URI("http://" + host + ":" + port);
		CloseableHttpClient delegate = mock(CloseableHttpClient.class);
		final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		StatusLine statusLine = mock(StatusLine.class);
		doReturn(200).when(statusLine).getStatusCode();
		doReturn(statusLine).when(response).getStatusLine();
		doThrow(new IOException("boom")).doThrow(new IOException("boom again")).doReturn(response).
			when(delegate).execute(any(HttpUriRequest.class));
		ILoadBalancer lb = mock(ILoadBalancer.class);
		MyBackOffPolicyFactory myBackOffPolicyFactory = new MyBackOffPolicyFactory();
		MyRetryListenersNotRetry myRetryListenersNotRetry = new MyRetryListenersNotRetry();
		RetryableRibbonLoadBalancingHttpClient client = setupClientForRetry(retriesNextServer, retriesSameServer, retryable, retryOnAllOps,
			serviceName, host, port, delegate, lb, "", myBackOffPolicyFactory, false, myRetryListenersNotRetry);
		RibbonApacheHttpRequest request = mock(RibbonApacheHttpRequest.class);
		doReturn(method).when(request).getMethod();
		doReturn(uri).when(request).getURI();
		doReturn(request).when(request).withNewUri(any(URI.class));
		HttpUriRequest uriRequest = mock(HttpUriRequest.class);
		doReturn(uriRequest).when(request).toRequest(any(RequestConfig.class));
		RibbonApacheHttpResponse returnedResponse = client.execute(request, null);

	}

	@Test
	public void retryWithOriginalConstructorTest() throws Exception {
		int retriesNextServer = 1;
		int retriesSameServer = 1;
		boolean retryable = true;
		boolean retryOnAllOps = true;
		String serviceName = "listener";
		String host = serviceName;
		int port = 80;
		HttpMethod method = HttpMethod.POST;
		URI uri = new URI("http://" + host + ":" + port);
		CloseableHttpClient delegate = mock(CloseableHttpClient.class);
		final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		StatusLine statusLine = mock(StatusLine.class);
		doReturn(200).when(statusLine).getStatusCode();
		doReturn(statusLine).when(response).getStatusLine();
		doThrow(new IOException("boom")).doThrow(new IOException("boom again")).doReturn(response).
			when(delegate).execute(any(HttpUriRequest.class));
		ILoadBalancer lb = mock(ILoadBalancer.class);
		MyBackOffPolicyFactory myBackOffPolicyFactory = new MyBackOffPolicyFactory();
		RetryableRibbonLoadBalancingHttpClient client = setupClientForRetry(retriesNextServer, retriesSameServer, retryable, retryOnAllOps,
			serviceName, host, port, delegate, lb, "", myBackOffPolicyFactory, false);
		RibbonApacheHttpRequest request = mock(RibbonApacheHttpRequest.class);
		doReturn(method).when(request).getMethod();
		doReturn(uri).when(request).getURI();
		doReturn(request).when(request).withNewUri(any(URI.class));
		HttpUriRequest uriRequest = mock(HttpUriRequest.class);
		doReturn(uriRequest).when(request).toRequest(any(RequestConfig.class));
		RibbonApacheHttpResponse returnedResponse = client.execute(request, null);
		verify(response, times(0)).close();
		verify(delegate, times(3)).execute(any(HttpUriRequest.class));
		verify(lb, times(2)).chooseServer(eq(serviceName));
		assertEquals(2, myBackOffPolicyFactory.getCount());
	}
	
	private RetryableRibbonLoadBalancingHttpClient setupClientForServerValidation(String serviceName, String host, int port,
			CloseableHttpClient delegate, ILoadBalancer lb) throws Exception {
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
		RetryableRibbonLoadBalancingHttpClient client = new RetryableRibbonLoadBalancingHttpClient(delegate,
				clientConfig, introspector, factory, loadBalancedBackOffPolicyFactory, loadBalancedRetryListenerFactory);
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
		CloseableHttpClient delegate = mock(CloseableHttpClient.class);
		ILoadBalancer lb = mock(ILoadBalancer.class);
		
		RetryableRibbonLoadBalancingHttpClient client = setupClientForServerValidation(serviceName, host, port, delegate, lb);
		RibbonApacheHttpRequest request = mock(RibbonApacheHttpRequest.class);
		doReturn(null).when(lb).chooseServer(eq(serviceName));
		doReturn(method).when(request).getMethod();
		doReturn(uri).when(request).getURI();
		doReturn(request).when(request).withNewUri(any(URI.class));
		HttpUriRequest uriRequest = mock(HttpUriRequest.class);
		doReturn(uriRequest).when(request).toRequest(any(RequestConfig.class));
		try {
			client.execute(request, null);
			fail("Expected IOException for no servers available");
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
		CloseableHttpClient delegate = mock(CloseableHttpClient.class);
		ILoadBalancer lb = mock(ILoadBalancer.class);
		
		RetryableRibbonLoadBalancingHttpClient client = setupClientForServerValidation(serviceName, host, port, delegate, lb);
		RibbonApacheHttpRequest request = mock(RibbonApacheHttpRequest.class);
		doReturn(new Server(null,8000)).when(lb).chooseServer(eq(serviceName));
		doReturn(method).when(request).getMethod();
		doReturn(uri).when(request).getURI();
		doReturn(request).when(request).withNewUri(any(URI.class));
		HttpUriRequest uriRequest = mock(HttpUriRequest.class);
		doReturn(uriRequest).when(request).toRequest(any(RequestConfig.class));
		try {
			client.execute(request, null);
			fail("Expected IOException for no servers available");
		} catch (ClientException ex) {
			assertThat(ex.getMessage(), containsString("Invalid Server for: "));
		}
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

		factory.setApplicationContext(new AnnotationConfigApplicationContext(HttpClientConfiguration.class,
				RibbonAutoConfiguration.class, defaultConfigurationClass));
		String serviceName = "foo";
		String host = serviceName;
		int port = 80;
		URI uri = new URI("http://" + host + ":" + port);
		CloseableHttpClient delegate = mock(CloseableHttpClient.class);
		RibbonLoadBalancingHttpClient client = factory.getClient("service",
				RibbonLoadBalancingHttpClient.class);

		ReflectionTestUtils.setField(client, "delegate", delegate);
		ReflectionTestUtils.setField(client, "lb", loadBalancer);
		CloseableHttpResponse httpResponse  = mock(CloseableHttpResponse.class);
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

	class MyBackOffPolicyFactory implements LoadBalancedBackOffPolicyFactory, BackOffPolicy {

		private int count = 0;

		@Override
		public BackOffContext start(RetryContext retryContext) {
			return null;
		}

		@Override
		public void backOff(BackOffContext backOffContext) throws BackOffInterruptedException {
			count++;
		}

		int getCount() {
			return count;
		}

		@Override
		public BackOffPolicy createBackOffPolicy(String service) {
			return this;
		}
	}

	class MyRetryListeners implements LoadBalancedRetryListenerFactory {

		private int onError = 0;

		@Override
		public RetryListener[] createRetryListeners(String service) {
			return new RetryListener[] {new RetryListener() {
				@Override
				public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
					return true;
				}

				@Override
				public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {

				}

				@Override
				public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
					onError++;
				}
			}};
		}

		int getOnError() {
			return onError;
		}
	}

	class MyRetryListenersNotRetry implements LoadBalancedRetryListenerFactory {

		@Override
		public RetryListener[] createRetryListeners(String service) {
			return new RetryListener[] {new RetryListener() {
				@Override
				public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
					return false;
				}

				@Override
				public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {

				}

				@Override
				public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {

				}
			}};
		}
	}

}
