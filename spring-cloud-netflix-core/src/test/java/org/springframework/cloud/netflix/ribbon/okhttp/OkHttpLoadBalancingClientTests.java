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

package org.springframework.cloud.netflix.ribbon.okhttp;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.ribbon.DefaultServerIntrospector;
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;

import okhttp3.OkHttpClient;

/**
 * @author Spencer Gibb
 */
public class OkHttpLoadBalancingClientTests {

	@Test
	public void testOkHttpClientUseDefaultsNoOverride() throws Exception {
		OkHttpClient result = getHttpClient(UseDefaults.class, null);

		assertThat(result.followRedirects(), is(false));
	}

	@Test
	public void testOkHttpClientDoNotFollowRedirectsNoOverride() throws Exception {
		OkHttpClient result = getHttpClient(DoNotFollowRedirects.class, null);

		assertThat(result.followRedirects(), is(false));
	}

	@Test
	public void testOkHttpClientFollowRedirectsNoOverride() throws Exception {
		OkHttpClient result = getHttpClient(FollowRedirects.class, null);

		assertThat(result.followRedirects(), is(true));
	}

	@Test
	public void testOkHttpClientDoNotFollowRedirectsOverrideWithFollowRedirects()
			throws Exception {

		DefaultClientConfigImpl override = new DefaultClientConfigImpl();
		override.set(CommonClientConfigKey.FollowRedirects, true);
		override.set(CommonClientConfigKey.IsSecure, false);

		OkHttpClient result = getHttpClient(DoNotFollowRedirects.class, override);

		assertThat(result.followRedirects(), is(true));
	}

	@Test
	public void testOkHttpClientFollowRedirectsOverrideWithDoNotFollowRedirects()
			throws Exception {

		DefaultClientConfigImpl override = new DefaultClientConfigImpl();
		override.set(CommonClientConfigKey.FollowRedirects, false);
		override.set(CommonClientConfigKey.IsSecure, false);

		OkHttpClient result = getHttpClient(FollowRedirects.class, override);

		assertThat(result.followRedirects(), is(false));
	}

	@Test
	public void testTimeouts() throws Exception {
		OkHttpClient result = getHttpClient(Timeouts.class, null);
		assertThat(result.readTimeoutMillis(), is(50000));
		assertThat(result.connectTimeoutMillis(), is(60000));
	}

	@Test
	public void testDefaultTimeouts() throws Exception {
		OkHttpClient result = getHttpClient(UseDefaults.class, null);
		assertThat(result.readTimeoutMillis(), is(1000));
		assertThat(result.connectTimeoutMillis(), is(1000));
	}

	@Test
	public void testTimeoutsOverride() throws Exception {
		DefaultClientConfigImpl override = new DefaultClientConfigImpl();
		override.set(CommonClientConfigKey.ConnectTimeout, 60);
		override.set(CommonClientConfigKey.ReadTimeout, 50);
		OkHttpClient result = getHttpClient(Timeouts.class, override);
		assertThat(result.readTimeoutMillis(), is(50));
		assertThat(result.connectTimeoutMillis(), is(60));
	}

	@Test
	public void testUpdatedTimeouts() throws Exception {
		SpringClientFactory factory = new SpringClientFactory();
		OkHttpClient result = getHttpClient(Timeouts.class, null, factory);
		assertThat(result.readTimeoutMillis(), is(50000));
		assertThat(result.connectTimeoutMillis(), is(60000));
		IClientConfig config = factory.getClientConfig("service");
		config.set(CommonClientConfigKey.ConnectTimeout, 60);
		config.set(CommonClientConfigKey.ReadTimeout, 50);
		result = getHttpClient(Timeouts.class, null, factory);
		assertThat(result.readTimeoutMillis(), is(50));
		assertThat(result.connectTimeoutMillis(), is(60));
	}

	private OkHttpClient getHttpClient(Class<?> defaultConfigurationClass,
											   IClientConfig configOverride) throws Exception {
		return getHttpClient(defaultConfigurationClass, configOverride, new SpringClientFactory());
	}

	private OkHttpClient getHttpClient(Class<?> defaultConfigurationClass,
									   IClientConfig configOverride,
									   SpringClientFactory factory) throws Exception {
		factory.setApplicationContext(new AnnotationConfigApplicationContext(
				RibbonAutoConfiguration.class, OkHttpClientConfiguration.class, defaultConfigurationClass));

		OkHttpLoadBalancingClient client = factory.getClient("service",
				OkHttpLoadBalancingClient.class);

		return client.getOkHttpClient(configOverride, false);
	}

	@Configuration
	protected static class OkHttpClientConfiguration {
		@Autowired(required = false)
		IClientConfig clientConfig;
		@Bean
		public OkHttpLoadBalancingClient okHttpLoadBalancingClient() {
			if(clientConfig == null) {
				clientConfig = new DefaultClientConfigImpl();
			}
			return new OkHttpLoadBalancingClient(new OkHttpClient(), clientConfig, new DefaultServerIntrospector());
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

}
