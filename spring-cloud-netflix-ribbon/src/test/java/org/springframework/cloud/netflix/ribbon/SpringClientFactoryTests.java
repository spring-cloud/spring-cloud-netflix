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

package org.springframework.cloud.netflix.ribbon;

import com.netflix.client.DefaultLoadBalancerRetryHandler;
import com.netflix.client.IClientConfigAware;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.niws.client.http.RestClient;
import com.sun.jersey.client.apache4.ApacheHttpClient4;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.junit.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.commons.httpclient.HttpClientConfiguration;
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 *
 */
public class SpringClientFactoryTests {

	@Test
	public void testConfigureRetry() {
		SpringClientFactory factory = new SpringClientFactory();
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext(
				RibbonAutoConfiguration.class, ArchaiusAutoConfiguration.class,
				HttpClientConfiguration.class);
		TestPropertyValues.of("foo.ribbon.MaxAutoRetries:2").applyTo(parent);
		factory.setApplicationContext(parent);
		DefaultLoadBalancerRetryHandler retryHandler = (DefaultLoadBalancerRetryHandler) factory
				.getLoadBalancerContext("foo").getRetryHandler();
		assertThat(retryHandler.getMaxRetriesOnSameServer()).isEqualTo(2);
		parent.close();
		factory.destroy();
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testCookiePolicy() {
		SpringClientFactory factory = new SpringClientFactory();
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("ribbon.restclient.enabled=true").applyTo(parent);
		parent.register(RibbonAutoConfiguration.class, ArchaiusAutoConfiguration.class);
		parent.refresh();
		factory.setApplicationContext(parent);
		RestClient client = factory.getClient("foo", RestClient.class);
		ApacheHttpClient4 jerseyClient = (ApacheHttpClient4) client.getJerseyClient();
		assertThat(jerseyClient.getClientHandler().getHttpClient().getParams()
				.getParameter(ClientPNames.COOKIE_POLICY))
						.isEqualTo(CookiePolicy.IGNORE_COOKIES);
		parent.close();
		factory.destroy();
	}

	@Test
	public void testInstantiateWithConfigInjectByConstructor() {
		IClientConfig clientConfig = new DefaultClientConfigImpl();
		ClientConfigInjectedByConstructor instance = SpringClientFactory
				.instantiateWithConfig(ClientConfigInjectedByConstructor.class,
						clientConfig);
		assertThat(instance.clientConfig).isSameAs(clientConfig);
	}

	@Test
	public void testInstantiateWithConfigInjectedByInitMethod() {
		IClientConfig clientConfig = new DefaultClientConfigImpl();
		ClientConfigInjectedByInitMethod instance = SpringClientFactory
				.instantiateWithConfig(ClientConfigInjectedByInitMethod.class,
						clientConfig);
		assertThat(instance.clientConfig).isSameAs(clientConfig);
	}

	@Test
	public void testInstantiateWithoutConfig() {
		IClientConfig clientConfig = new DefaultClientConfigImpl();
		NoClientConfigAware instance = SpringClientFactory
				.instantiateWithConfig(NoClientConfigAware.class, clientConfig);
		assertThat(instance).isNotNull();
	}

	public static class ClientConfigInjectedByConstructor {

		private IClientConfig clientConfig;

		public ClientConfigInjectedByConstructor(IClientConfig clientConfig) {
			this.clientConfig = clientConfig;
		}

	}

	public static class ClientConfigInjectedByInitMethod implements IClientConfigAware {

		private IClientConfig clientConfig;

		@Override
		public void initWithNiwsConfig(IClientConfig clientConfig) {
			this.clientConfig = clientConfig;
		}

	}

	public static class NoClientConfigAware {

		public NoClientConfigAware() {
			// no client config
		}

	}

}
