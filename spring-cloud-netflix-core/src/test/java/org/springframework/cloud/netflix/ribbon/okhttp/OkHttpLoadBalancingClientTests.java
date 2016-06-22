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

	private OkHttpClient getHttpClient(Class<?> defaultConfigurationClass,
											   IClientConfig configOverride) throws Exception {
		SpringClientFactory factory = new SpringClientFactory();
		factory.setApplicationContext(new AnnotationConfigApplicationContext(
				defaultConfigurationClass));

		OkHttpLoadBalancingClient client = factory.getClient("service",
				OkHttpLoadBalancingClient.class);

		return client.getOkHttpClient(configOverride, false);
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

}
