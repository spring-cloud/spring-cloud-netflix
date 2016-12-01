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

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Sébastien Nussbaumer
 */
public class RibbonLoadBalancingHttpClientTests {

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
		RibbonLoadBalancingHttpClient client = factory.getClient("service",
				RibbonLoadBalancingHttpClient.class);

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
		HttpClient delegate = mock(HttpClient.class);
		RibbonLoadBalancingHttpClient client = factory.getClient("service",
				RibbonLoadBalancingHttpClient.class);

		ReflectionTestUtils.setField(client, "delegate", delegate);
		given(delegate.execute(any(HttpUriRequest.class))).willReturn(
				mock(HttpResponse.class));
		RibbonApacheHttpRequest request = mock(RibbonApacheHttpRequest.class);
		given(request.toRequest(any(RequestConfig.class))).willReturn(
				mock(HttpUriRequest.class));

		client.execute(request, configOverride);

		ArgumentCaptor<RequestConfig> requestConfigCaptor = ArgumentCaptor
				.forClass(RequestConfig.class);
		verify(request).toRequest(requestConfigCaptor.capture());
		return requestConfigCaptor.getValue();
	}

}
