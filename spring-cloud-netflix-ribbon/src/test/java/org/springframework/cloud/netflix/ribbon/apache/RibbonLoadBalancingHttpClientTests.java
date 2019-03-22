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

package org.springframework.cloud.netflix.ribbon.apache;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.netflix.client.ClientException;
import com.netflix.client.DefaultLoadBalancerRetryHandler;
import com.netflix.client.RetryHandler;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AbstractLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.LoadBalancerStats;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerStats;
import com.netflix.servo.monitor.Monitors;
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

import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.commons.httpclient.HttpClientConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancedRetryFactory;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancedRetryPolicy;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerContext;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.ribbon.support.RibbonCommandContext;
import org.springframework.cloud.netflix.ribbon.support.RibbonRequestCustomizer;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
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
import static org.mockito.Mockito.when;

/**
 * @author Sébastien Nussbaumer
 * @author Ryan Baxter
 * @author Gang Li
 */
public class RibbonLoadBalancingHttpClientTests {

	private ILoadBalancer loadBalancer;

	@Before
	public void setup() {
		loadBalancer = mock(AbstractLoadBalancer.class);
		doReturn(new Server("foo.com", 8000)).when(loadBalancer)
				.chooseServer(eq("default"));
		doReturn(new Server("foo.com", 8000)).when(loadBalancer)
				.chooseServer(eq("service"));
	}

	@After
	public void teardown() {
		loadBalancer = null;
	}

	@Test
	public void testRequestConfigUseDefaultsNoOverride() throws Exception {
		RequestConfig result = getBuiltRequestConfig(UseDefaults.class, null);

		assertThat(result.isRedirectsEnabled()).isFalse();

	}

	@Test
	public void testRequestConfigDoNotFollowRedirectsNoOverride() throws Exception {
		RequestConfig result = getBuiltRequestConfig(DoNotFollowRedirects.class, null);

		assertThat(result.isRedirectsEnabled()).isFalse();
	}

	@Test
	public void testRequestConfigFollowRedirectsNoOverride() throws Exception {
		RequestConfig result = getBuiltRequestConfig(FollowRedirects.class, null);

		assertThat(result.isRedirectsEnabled()).isTrue();
	}

	@Test
	public void testTimeouts() throws Exception {
		RequestConfig result = getBuiltRequestConfig(Timeouts.class, null);
		assertThat(result.getConnectTimeout()).isEqualTo(60000);
		assertThat(result.getSocketTimeout()).isEqualTo(50000);
	}

	@Test
	public void testDefaultTimeouts() throws Exception {
		RequestConfig result = getBuiltRequestConfig(UseDefaults.class, null);
		assertThat(result.getConnectTimeout()).isEqualTo(1000);
		assertThat(result.getSocketTimeout()).isEqualTo(1000);
	}

	@Test
	public void testCompressionDefault() throws Exception {
		RequestConfig result = getBuiltRequestConfig(UseDefaults.class, null);
		assertThat(result.isContentCompressionEnabled()).isTrue();
	}

	@Test
	public void testCompressionDisabled() throws Exception {
		IClientConfig configOverride = DefaultClientConfigImpl
				.getClientConfigWithDefaultValues();
		configOverride.set(CommonClientConfigKey.GZipPayload, false);
		RequestConfig result = getBuiltRequestConfig(UseDefaults.class, configOverride);
		assertThat(result.isContentCompressionEnabled()).isFalse();
	}

	@Test
	public void testConnections() throws Exception {
		SpringClientFactory factory = new SpringClientFactory();
		factory.setApplicationContext(new AnnotationConfigApplicationContext(
				RibbonAutoConfiguration.class, Connections.class));
		RetryableRibbonLoadBalancingHttpClient client = factory.getClient("service",
				RetryableRibbonLoadBalancingHttpClient.class);

		HttpClient delegate = client.getDelegate();
		PoolingHttpClientConnectionManager connManager = (PoolingHttpClientConnectionManager) ReflectionTestUtils
				.getField(delegate, "connManager");
		assertThat(connManager.getMaxTotal()).isEqualTo(101);
		assertThat(connManager.getDefaultMaxPerRoute()).isEqualTo(201);
	}

	@Test
	public void testRequestConfigDoNotFollowRedirectsOverrideWithFollowRedirects()
			throws Exception {

		DefaultClientConfigImpl override = new DefaultClientConfigImpl();
		override.set(CommonClientConfigKey.FollowRedirects, true);
		override.set(CommonClientConfigKey.IsSecure, false);

		RequestConfig result = getBuiltRequestConfig(DoNotFollowRedirects.class,
				override);

		assertThat(result.isRedirectsEnabled()).isTrue();
	}

	@Test
	public void testRequestConfigFollowRedirectsOverrideWithDoNotFollowRedirects()
			throws Exception {

		DefaultClientConfigImpl override = new DefaultClientConfigImpl();
		override.set(CommonClientConfigKey.FollowRedirects, false);
		override.set(CommonClientConfigKey.IsSecure, false);

		RequestConfig result = getBuiltRequestConfig(FollowRedirects.class, override);

		assertThat(result.isRedirectsEnabled()).isFalse();
	}

	@Test
	public void testUpdatedTimeouts() throws Exception {
		SpringClientFactory factory = new SpringClientFactory();
		RequestConfig result = getBuiltRequestConfig(Timeouts.class, null, factory);
		assertThat(result.getConnectTimeout()).isEqualTo(60000);
		assertThat(result.getSocketTimeout()).isEqualTo(50000);
		IClientConfig config = factory.getClientConfig("service");
		config.set(CommonClientConfigKey.ConnectTimeout, 60);
		config.set(CommonClientConfigKey.ReadTimeout, 50);
		result = getBuiltRequestConfig(Timeouts.class, null, factory);
		assertThat(result.getConnectTimeout()).isEqualTo(60);
		assertThat(result.getSocketTimeout()).isEqualTo(50);
	}

	@Test
	public void testNeverRetry() throws Exception {
		ServerIntrospector introspector = mock(ServerIntrospector.class);
		CloseableHttpClient delegate = mock(CloseableHttpClient.class);
		doThrow(new IOException("boom")).when(delegate)
				.execute(any(HttpUriRequest.class));
		DefaultClientConfigImpl clientConfig = new DefaultClientConfigImpl();
		clientConfig.setClientName("foo");
		RibbonLoadBalancingHttpClient client = new RibbonLoadBalancingHttpClient(delegate,
				clientConfig, introspector);
		RibbonApacheHttpRequest request = mock(RibbonApacheHttpRequest.class);
		when(request.toRequest(any(RequestConfig.class)))
				.thenReturn(mock(HttpUriRequest.class));
		try {
			client.execute(request, null);
			fail("Expected IOException");
		}
		catch (IOException e) {
		}
		finally {
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
				doReturn(new ByteArrayInputStream("test".getBytes())).when(entity)
						.getContent();
				return entity;
			}
		}).when(fourOFourResponse).getEntity();
		doReturn(404).when(fourOFourStatusLine).getStatusCode();
		doReturn(fourOFourStatusLine).when(fourOFourResponse).getStatusLine();
		doReturn(locale).when(fourOFourResponse).getLocale();
		doReturn(fourOFourResponse).when(delegate).execute(any(HttpUriRequest.class));
		ILoadBalancer lb = mock(ILoadBalancer.class);
		RibbonLoadBalancerContext ribbonLoadBalancerContext = new RibbonLoadBalancerContext(
				lb);
		MyBackOffPolicy myBackOffPolicy = new MyBackOffPolicy();
		RetryableRibbonLoadBalancingHttpClient client = setupClientForRetry(
				retriesNextServer, retriesSameServer, retryable, retryOnAllOps,
				serviceName, host, port, delegate, lb, "404", myBackOffPolicy);
		client.setRibbonLoadBalancerContext(ribbonLoadBalancerContext);
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
		assertThat(new String(buf, 0, read)).isEqualTo("test");
	}

	private RetryableRibbonLoadBalancingHttpClient setupClientForRetry(
			int retriesNextServer, int retriesSameServer, boolean retryable,
			boolean retryOnAllOps, String serviceName, String host, int port,
			CloseableHttpClient delegate, ILoadBalancer lb, String statusCodes,
			BackOffPolicy backOffPolicy) throws Exception {
		return setupClientForRetry(retriesNextServer, retriesSameServer, retryable,
				retryOnAllOps, serviceName, host, port, delegate, lb, statusCodes,
				backOffPolicy, false);
	}

	private RetryableRibbonLoadBalancingHttpClient setupClientForRetry(
			int retriesNextServer, int retriesSameServer, boolean retryable,
			boolean retryOnAllOps, String serviceName, String host, int port,
			CloseableHttpClient delegate, ILoadBalancer lb, String statusCodes,
			BackOffPolicy backOffPolicy, boolean isSecure) throws Exception {
		ServerIntrospector introspector = mock(ServerIntrospector.class);
		RetryHandler retryHandler = new DefaultLoadBalancerRetryHandler(retriesSameServer,
				retriesNextServer, retryable);
		doReturn(new Server(host, port)).when(lb).chooseServer(eq(serviceName));
		DefaultClientConfigImpl clientConfig = new DefaultClientConfigImpl();
		clientConfig.set(CommonClientConfigKey.OkToRetryOnAllOperations, retryOnAllOps);
		clientConfig.set(CommonClientConfigKey.MaxAutoRetriesNextServer,
				retriesNextServer);
		clientConfig.set(CommonClientConfigKey.MaxAutoRetries, retriesSameServer);
		clientConfig.set(RibbonLoadBalancedRetryPolicy.RETRYABLE_STATUS_CODES,
				statusCodes);
		clientConfig.set(CommonClientConfigKey.IsSecure, isSecure);
		clientConfig.setClientName(serviceName);
		RibbonLoadBalancerContext context = new RibbonLoadBalancerContext(lb,
				clientConfig, retryHandler);
		SpringClientFactory clientFactory = mock(SpringClientFactory.class);
		doReturn(context).when(clientFactory).getLoadBalancerContext(eq(serviceName));
		doReturn(clientConfig).when(clientFactory).getClientConfig(eq(serviceName));
		LoadBalancedRetryFactory factory = new RibbonLoadBalancedRetryFactory(
				clientFactory) {
			@Override
			public BackOffPolicy createBackOffPolicy(String service) {
				return backOffPolicy;
			}
		};
		RetryableRibbonLoadBalancingHttpClient client = new RetryableRibbonLoadBalancingHttpClient(
				delegate, clientConfig, introspector, factory);
		client.setLoadBalancer(lb);
		ReflectionTestUtils.setField(client, "delegate", delegate);
		return client;
	}

	private RetryableRibbonLoadBalancingHttpClient setupClientForRetry(
			int retriesNextServer, int retriesSameServer, boolean retryable,
			boolean retryOnAllOps, String serviceName, String host, int port,
			CloseableHttpClient delegate, ILoadBalancer lb, String statusCodes,
			BackOffPolicy backOffPolicy, boolean isSecure, RetryListener[] retryListeners)
			throws Exception {
		ServerIntrospector introspector = mock(ServerIntrospector.class);
		RetryHandler retryHandler = new DefaultLoadBalancerRetryHandler(retriesSameServer,
				retriesNextServer, retryable);
		doReturn(new Server(host, port)).when(lb).chooseServer(eq(serviceName));
		DefaultClientConfigImpl clientConfig = new DefaultClientConfigImpl();
		clientConfig.set(CommonClientConfigKey.OkToRetryOnAllOperations, retryOnAllOps);
		clientConfig.set(CommonClientConfigKey.MaxAutoRetriesNextServer,
				retriesNextServer);
		clientConfig.set(CommonClientConfigKey.MaxAutoRetries, retriesSameServer);
		clientConfig.set(RibbonLoadBalancedRetryPolicy.RETRYABLE_STATUS_CODES,
				statusCodes);
		clientConfig.set(CommonClientConfigKey.IsSecure, isSecure);
		clientConfig.setClientName(serviceName);
		RibbonLoadBalancerContext context = new RibbonLoadBalancerContext(lb,
				clientConfig, retryHandler);
		SpringClientFactory clientFactory = mock(SpringClientFactory.class);
		doReturn(context).when(clientFactory).getLoadBalancerContext(eq(serviceName));
		doReturn(clientConfig).when(clientFactory).getClientConfig(eq(serviceName));
		LoadBalancedRetryFactory factory = new RibbonLoadBalancedRetryFactory(
				clientFactory) {
			@Override
			public RetryListener[] createRetryListeners(String service) {
				return retryListeners;
			}

			@Override
			public BackOffPolicy createBackOffPolicy(String service) {
				return backOffPolicy;
			}
		};
		RetryableRibbonLoadBalancingHttpClient client = new RetryableRibbonLoadBalancingHttpClient(
				delegate, clientConfig, introspector, factory);
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
		doThrow(new IOException("boom")).doReturn(response).when(delegate)
				.execute(any(HttpUriRequest.class));
		AbstractLoadBalancer lb = mock(AbstractLoadBalancer.class);
		LoadBalancerStats lbStats = mock(LoadBalancerStats.class);
		doReturn(lbStats).when(lb).getLoadBalancerStats();
		ServerStats serverStats = mock(ServerStats.class);
		doReturn(serverStats).when(lbStats).getSingleServerStat(any(Server.class));
		RibbonLoadBalancerContext ribbonLoadBalancerContext = mock(
				RibbonLoadBalancerContext.class);
		doReturn(lb).when(ribbonLoadBalancerContext).getLoadBalancer();
		doReturn(Monitors.newTimer("_LoadBalancerExecutionTimer", TimeUnit.MILLISECONDS))
				.when(ribbonLoadBalancerContext).getExecuteTracer();
		RetryableRibbonLoadBalancingHttpClient client = setupClientForRetry(
				retriesNextServer, retriesSameServer, retryable, retryOnAllOps,
				serviceName, host, port, delegate, lb, "", null);
		client.setRibbonLoadBalancerContext(ribbonLoadBalancerContext);
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
		verify(ribbonLoadBalancerContext, times(1)).noteRequestCompletion(serverStats,
				response, null, 0, null);
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
		URI uri = new URI("http://" + host + ":" + port + "/a%2Bb");
		CloseableHttpClient delegate = mock(CloseableHttpClient.class);
		final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		StatusLine statusLine = mock(StatusLine.class);
		doReturn(200).when(statusLine).getStatusCode();
		doReturn(statusLine).when(response).getStatusLine();
		doThrow(new IOException("boom")).doThrow(new IOException("boom again"))
				.doReturn(response).when(delegate).execute(any(HttpUriRequest.class));
		ILoadBalancer lb = mock(ILoadBalancer.class);
		RibbonLoadBalancerContext ribbonLoadBalancerContext = new RibbonLoadBalancerContext(
				lb);
		MyBackOffPolicy myBackOffPolicy = new MyBackOffPolicy();
		RetryableRibbonLoadBalancingHttpClient client = setupClientForRetry(
				retriesNextServer, retriesSameServer, retryable, retryOnAllOps,
				serviceName, host, port, delegate, lb, "", myBackOffPolicy);
		client.setRibbonLoadBalancerContext(ribbonLoadBalancerContext);
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
		assertThat(myBackOffPolicy.getCount()).isEqualTo(2);
		verify(request, times(3)).withNewUri(argThat(new ArgumentMatcher<URI>() {
			@Override
			public boolean matches(URI argument) {
				if (argument.equals(uri)) {
					return true;
				}
				return false;
			}
		}));
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
		doThrow(new IOException("boom")).doThrow(new IOException("boom again"))
				.doReturn(response).when(delegate).execute(any(HttpUriRequest.class));
		ILoadBalancer lb = mock(ILoadBalancer.class);
		RibbonLoadBalancerContext ribbonLoadBalancerContext = new RibbonLoadBalancerContext(
				lb);
		MyBackOffPolicy myBackOffPolicy = new MyBackOffPolicy();
		RetryableRibbonLoadBalancingHttpClient client = setupClientForRetry(
				retriesNextServer, retriesSameServer, retryable, retryOnAllOps,
				serviceName, host, port, delegate, lb, "", myBackOffPolicy);
		client.setRibbonLoadBalancerContext(ribbonLoadBalancerContext);
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
		assertThat(myBackOffPolicy.getCount()).isEqualTo(2);
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
		RibbonCommandContext context = new RibbonCommandContext(serviceName,
				method.toString(), uri.toString(), false,
				new LinkedMultiValueMap<String, String>(),
				new LinkedMultiValueMap<String, String>(),
				new ByteArrayInputStream("bar".getBytes()),
				new ArrayList<RibbonRequestCustomizer>());
		RibbonApacheHttpRequest request = new RibbonApacheHttpRequest(context);
		CloseableHttpClient delegate = mock(CloseableHttpClient.class);
		final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		StatusLine statusLine = mock(StatusLine.class);
		doReturn(200).when(statusLine).getStatusCode();
		doReturn(statusLine).when(response).getStatusLine();
		doReturn(response).when(delegate).execute(any(HttpUriRequest.class));
		RibbonLoadBalancingHttpClient client = new RibbonLoadBalancingHttpClient(delegate,
				clientConfig, introspector);
		client.execute(request, null);
		verify(response, times(0)).close();
		verify(delegate, times(1)).execute(argThat(new ArgumentMatcher<HttpUriRequest>() {
			@Override
			public boolean matches(HttpUriRequest argument) {
				if (argument instanceof HttpUriRequest) {
					HttpUriRequest arg = (HttpUriRequest) argument;
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
		RibbonCommandContext context = new RibbonCommandContext(serviceName,
				method.toString(), uri.toString(), true,
				new LinkedMultiValueMap<String, String>(),
				new LinkedMultiValueMap<String, String>(),
				new ByteArrayInputStream(new String("bar").getBytes()),
				new ArrayList<RibbonRequestCustomizer>());
		RibbonApacheHttpRequest request = new RibbonApacheHttpRequest(context);
		CloseableHttpClient delegate = mock(CloseableHttpClient.class);
		final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		StatusLine statusLine = mock(StatusLine.class);
		doReturn(200).when(statusLine).getStatusCode();
		doReturn(statusLine).when(response).getStatusLine();
		doReturn(response).when(delegate).execute(any(HttpUriRequest.class));
		ILoadBalancer lb = mock(ILoadBalancer.class);
		RibbonLoadBalancerContext ribbonLoadBalancerContext = new RibbonLoadBalancerContext(
				lb);
		RetryableRibbonLoadBalancingHttpClient client = setupClientForRetry(
				retriesNextServer, retriesSameServer, retryable, retryOnAllOps,
				serviceName, host, port, delegate, lb, "", null, true);
		client.setRibbonLoadBalancerContext(ribbonLoadBalancerContext);
		client.execute(request, null);
		verify(response, times(0)).close();
		verify(delegate, times(1)).execute(argThat(new ArgumentMatcher<HttpUriRequest>() {
			@Override
			public boolean matches(HttpUriRequest argument) {
				if (argument instanceof HttpUriRequest) {
					HttpUriRequest arg = (HttpUriRequest) argument;
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
		doThrow(new IOException("boom")).doThrow(new IOException("boom again"))
				.doReturn(response).when(delegate).execute(any(HttpUriRequest.class));
		ILoadBalancer lb = mock(ILoadBalancer.class);
		RibbonLoadBalancerContext ribbonLoadBalancerContext = new RibbonLoadBalancerContext(
				lb);
		RetryableRibbonLoadBalancingHttpClient client = setupClientForRetry(
				retriesNextServer, retriesSameServer, retryable, retryOnAllOps,
				serviceName, host, port, delegate, lb, "", null);
		client.setRibbonLoadBalancerContext(ribbonLoadBalancerContext);
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
		}
		catch (IOException e) {
		}
		finally {
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
		doReturn(fourOFourResponse).doReturn(response).when(delegate)
				.execute(any(HttpUriRequest.class));
		ILoadBalancer lb = mock(ILoadBalancer.class);
		RibbonLoadBalancerContext ribbonLoadBalancerContext = new RibbonLoadBalancerContext(
				lb);
		MyBackOffPolicy myBackOffPolicy = new MyBackOffPolicy();
		RetryableRibbonLoadBalancingHttpClient client = setupClientForRetry(
				retriesNextServer, retriesSameServer, retryable, retryOnAllOps,
				serviceName, host, port, delegate, lb, "404", myBackOffPolicy);
		client.setRibbonLoadBalancerContext(ribbonLoadBalancerContext);
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
		assertThat(myBackOffPolicy.getCount()).isEqualTo(1);
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
		doThrow(new IOException("boom")).doThrow(new IOException("boom again"))
				.doReturn(response).when(delegate).execute(any(HttpUriRequest.class));
		ILoadBalancer lb = mock(ILoadBalancer.class);
		RibbonLoadBalancerContext ribbonLoadBalancerContext = new RibbonLoadBalancerContext(
				lb);
		MyBackOffPolicy myBackOffPolicy = new MyBackOffPolicy();
		MyRetryListener myRetryListener = new MyRetryListener();
		RetryableRibbonLoadBalancingHttpClient client = setupClientForRetry(
				retriesNextServer, retriesSameServer, retryable, retryOnAllOps,
				serviceName, host, port, delegate, lb, "", myBackOffPolicy, false,
				new RetryListener[] { myRetryListener });
		client.setRibbonLoadBalancerContext(ribbonLoadBalancerContext);
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
		assertThat(myBackOffPolicy.getCount()).isEqualTo(2);
		assertThat(myRetryListener.getOnError()).isEqualTo(2);
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
		doThrow(new IOException("boom")).doThrow(new IOException("boom again"))
				.doReturn(response).when(delegate).execute(any(HttpUriRequest.class));
		ILoadBalancer lb = mock(ILoadBalancer.class);
		RibbonLoadBalancerContext ribbonLoadBalancerContext = new RibbonLoadBalancerContext(
				lb);
		MyBackOffPolicy myBackOffPolicy = new MyBackOffPolicy();
		MyRetryListener myRetryListener = new MyRetryListener();
		RetryableRibbonLoadBalancingHttpClient client = setupClientForRetry(
				retriesNextServer, retriesSameServer, retryable, retryOnAllOps,
				serviceName, host, port, delegate, lb, "", myBackOffPolicy, false,
				new RetryListener[] {});
		client.setRibbonLoadBalancerContext(ribbonLoadBalancerContext);
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
		assertThat(myBackOffPolicy.getCount()).isEqualTo(2);
		assertThat(myRetryListener.getOnError()).isEqualTo(0);
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
		doThrow(new IOException("boom")).doThrow(new IOException("boom again"))
				.doReturn(response).when(delegate).execute(any(HttpUriRequest.class));
		ILoadBalancer lb = mock(ILoadBalancer.class);
		MyBackOffPolicy myBackOffPolicy = new MyBackOffPolicy();
		MyRetryListenerNotRetry myRetryListenerNotRetry = new MyRetryListenerNotRetry();
		RetryableRibbonLoadBalancingHttpClient client = setupClientForRetry(
				retriesNextServer, retriesSameServer, retryable, retryOnAllOps,
				serviceName, host, port, delegate, lb, "", myBackOffPolicy, false,
				new RetryListener[] { myRetryListenerNotRetry });
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
		doThrow(new IOException("boom")).doThrow(new IOException("boom again"))
				.doReturn(response).when(delegate).execute(any(HttpUriRequest.class));
		ILoadBalancer lb = mock(ILoadBalancer.class);
		RibbonLoadBalancerContext ribbonLoadBalancerContext = new RibbonLoadBalancerContext(
				lb);
		MyBackOffPolicy myBackOffPolicy = new MyBackOffPolicy();
		RetryableRibbonLoadBalancingHttpClient client = setupClientForRetry(
				retriesNextServer, retriesSameServer, retryable, retryOnAllOps,
				serviceName, host, port, delegate, lb, "", myBackOffPolicy, false);
		client.setRibbonLoadBalancerContext(ribbonLoadBalancerContext);
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
		assertThat(myBackOffPolicy.getCount()).isEqualTo(2);
	}

	private RetryableRibbonLoadBalancingHttpClient setupClientForServerValidation(
			String serviceName, String host, int port, CloseableHttpClient delegate,
			ILoadBalancer lb) throws Exception {
		ServerIntrospector introspector = mock(ServerIntrospector.class);
		RetryHandler retryHandler = new DefaultLoadBalancerRetryHandler(1, 1, true);
		DefaultClientConfigImpl clientConfig = new DefaultClientConfigImpl();
		clientConfig.set(CommonClientConfigKey.OkToRetryOnAllOperations, true);
		clientConfig.set(CommonClientConfigKey.MaxAutoRetriesNextServer, 0);
		clientConfig.set(CommonClientConfigKey.MaxAutoRetries, 1);
		clientConfig.set(RibbonLoadBalancedRetryPolicy.RETRYABLE_STATUS_CODES, "");
		clientConfig.set(CommonClientConfigKey.IsSecure, false);
		clientConfig.setClientName(serviceName);
		RibbonLoadBalancerContext context = new RibbonLoadBalancerContext(lb,
				clientConfig, retryHandler);
		SpringClientFactory clientFactory = mock(SpringClientFactory.class);
		doReturn(context).when(clientFactory).getLoadBalancerContext(eq(serviceName));
		doReturn(clientConfig).when(clientFactory).getClientConfig(eq(serviceName));
		LoadBalancedRetryFactory factory = new RibbonLoadBalancedRetryFactory(
				clientFactory);
		RetryableRibbonLoadBalancingHttpClient client = new RetryableRibbonLoadBalancingHttpClient(
				delegate, clientConfig, introspector, factory);
		client.setLoadBalancer(lb);
		client.setRibbonLoadBalancerContext(context);
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

		RetryableRibbonLoadBalancingHttpClient client = setupClientForServerValidation(
				serviceName, host, port, delegate, lb);
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
		}
		catch (ClientException ex) {
			assertThat(ex.getMessage())
					.contains("Load balancer does not have available server for client");
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

		RetryableRibbonLoadBalancingHttpClient client = setupClientForServerValidation(
				serviceName, host, port, delegate, lb);
		RibbonApacheHttpRequest request = mock(RibbonApacheHttpRequest.class);
		doReturn(new Server(null, 8000)).when(lb).chooseServer(eq(serviceName));
		doReturn(method).when(request).getMethod();
		doReturn(uri).when(request).getURI();
		doReturn(request).when(request).withNewUri(any(URI.class));
		HttpUriRequest uriRequest = mock(HttpUriRequest.class);
		doReturn(uriRequest).when(request).toRequest(any(RequestConfig.class));
		try {
			client.execute(request, null);
			fail("Expected IOException for no servers available");
		}
		catch (ClientException ex) {
			assertThat(ex.getMessage()).contains("Invalid Server for: ");
		}
	}

	private RequestConfig getBuiltRequestConfig(Class<?> defaultConfigurationClass,
			IClientConfig configOverride) throws Exception {
		return getBuiltRequestConfig(defaultConfigurationClass, configOverride,
				new SpringClientFactory());
	}

	private RequestConfig getBuiltRequestConfig(Class<?> defaultConfigurationClass,
			IClientConfig configOverride, SpringClientFactory factory) throws Exception {

		factory.setApplicationContext(
				new AnnotationConfigApplicationContext(HttpClientConfiguration.class,
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
		CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
		StatusLine statusLine = mock(StatusLine.class);
		doReturn(200).when(statusLine).getStatusCode();
		doReturn(statusLine).when(httpResponse).getStatusLine();
		given(delegate.execute(any(HttpUriRequest.class))).willReturn(httpResponse);
		RibbonApacheHttpRequest request = mock(RibbonApacheHttpRequest.class);
		doReturn(uri).when(request).getURI();
		doReturn(request).when(request).withNewUri(any(URI.class));
		given(request.toRequest(any(RequestConfig.class)))
				.willReturn(mock(HttpUriRequest.class));

		client.execute(request, configOverride);

		ArgumentCaptor<RequestConfig> requestConfigCaptor = ArgumentCaptor
				.forClass(RequestConfig.class);
		verify(request, times(1)).toRequest(requestConfigCaptor.capture());
		return requestConfigCaptor.getValue();
	}

	class MyBackOffPolicy implements BackOffPolicy {

		private int count = 0;

		@Override
		public BackOffContext start(RetryContext retryContext) {
			return null;
		}

		@Override
		public void backOff(BackOffContext backOffContext)
				throws BackOffInterruptedException {
			count++;
		}

		int getCount() {
			return count;
		}

	}

	class MyRetryListener implements RetryListener {

		private int onError = 0;

		int getOnError() {
			return onError;
		}

		@Override
		public <T, E extends Throwable> boolean open(RetryContext context,
				RetryCallback<T, E> callback) {
			return true;
		}

		@Override
		public <T, E extends Throwable> void close(RetryContext context,
				RetryCallback<T, E> callback, Throwable throwable) {

		}

		@Override
		public <T, E extends Throwable> void onError(RetryContext context,
				RetryCallback<T, E> callback, Throwable throwable) {
			onError++;
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
	protected static class Connections {

		@Bean
		public IClientConfig clientConfig() {
			DefaultClientConfigImpl config = new DefaultClientConfigImpl();
			config.set(CommonClientConfigKey.MaxTotalConnections, 101);
			config.set(CommonClientConfigKey.MaxConnectionsPerHost, 201);
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

	class MyRetryListenerNotRetry implements RetryListener {

		@Override
		public <T, E extends Throwable> boolean open(RetryContext context,
				RetryCallback<T, E> callback) {
			return false;
		}

		@Override
		public <T, E extends Throwable> void close(RetryContext context,
				RetryCallback<T, E> callback, Throwable throwable) {

		}

		@Override
		public <T, E extends Throwable> void onError(RetryContext context,
				RetryCallback<T, E> callback, Throwable throwable) {

		}

	}

}
