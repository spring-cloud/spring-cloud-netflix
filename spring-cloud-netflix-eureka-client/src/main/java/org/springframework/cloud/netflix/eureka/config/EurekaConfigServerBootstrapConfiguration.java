/*
 * Copyright 2013-2022 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.config;

import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.transport.EurekaHttpClient;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.http.codec.CodecsAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.config.client.ConfigServerInstanceProvider;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.cloud.configuration.TlsProperties;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.http.DefaultEurekaClientHttpRequestFactorySupplier;
import org.springframework.cloud.netflix.eureka.http.EurekaClientHttpRequestFactorySupplier;
import org.springframework.cloud.netflix.eureka.http.RestTemplateEurekaHttpClient;
import org.springframework.cloud.netflix.eureka.http.RestTemplateTransportClientFactory;
import org.springframework.cloud.netflix.eureka.http.WebClientEurekaHttpClient;
import org.springframework.cloud.netflix.eureka.http.WebClientTransportClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Bootstrap configuration for config client that wants to lookup the config server via
 * discovery.
 *
 * @author Dave Syer
 */
@ConditionalOnClass(ConfigServicePropertySourceLocator.class)
@Conditional(EurekaConfigServerBootstrapConfiguration.EurekaConfigServerBootstrapCondition.class)
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties
public class EurekaConfigServerBootstrapConfiguration {

	@Bean
	@ConditionalOnMissingBean(value = EurekaClientConfig.class, search = SearchStrategy.CURRENT)
	public EurekaClientConfigBean eurekaClientConfigBean() {
		return new EurekaClientConfigBean();
	}

	@Bean
	@ConditionalOnMissingBean(EurekaHttpClient.class)
	@ConditionalOnProperty(prefix = "eureka.client", name = "webclient.enabled", matchIfMissing = true,
			havingValue = "false")
	public RestTemplateEurekaHttpClient configDiscoveryRestTemplateEurekaHttpClient(EurekaClientConfigBean config,
			Environment env, @Nullable TlsProperties properties,
			EurekaClientHttpRequestFactorySupplier eurekaClientHttpRequestFactorySupplier) {
		return (RestTemplateEurekaHttpClient) new RestTemplateTransportClientFactory(properties,
				eurekaClientHttpRequestFactorySupplier)
						.newClient(HostnameBasedUrlRandomizer.randomEndpoint(config, env));
	}

	@Bean
	@ConditionalOnMissingBean
	EurekaClientHttpRequestFactorySupplier defaultEurekaClientHttpRequestFactorySupplier() {
		return new DefaultEurekaClientHttpRequestFactorySupplier();
	}

	@Bean
	public ConfigServerInstanceProvider.Function eurekaConfigServerInstanceProvider(EurekaHttpClient client,
			EurekaClientConfig config) {
		return new EurekaConfigServerInstanceProvider(client, config)::getInstances;
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "org.springframework.web.reactive.function.client.WebClient")
	@ConditionalOnProperty(prefix = "eureka.client", name = "webclient.enabled", havingValue = "true")
	@ImportAutoConfiguration({ CodecsAutoConfiguration.class, WebClientAutoConfiguration.class })
	protected static class WebClientConfiguration {

		@Bean
		@ConditionalOnMissingBean(EurekaHttpClient.class)
		public WebClientEurekaHttpClient configDiscoveryWebClientEurekaHttpClient(EurekaClientConfigBean config,
				ObjectProvider<WebClient.Builder> builder, Environment env) {
			return (WebClientEurekaHttpClient) new WebClientTransportClientFactory(builder::getIfAvailable)
					.newClient(HostnameBasedUrlRandomizer.randomEndpoint(config, env));
		}

	}

	static class EurekaConfigServerBootstrapCondition extends AllNestedConditions {

		EurekaConfigServerBootstrapCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnProperty("spring.cloud.config.discovery.enabled")
		static class OnCloudConfigProperty {

		}

		@ConditionalOnProperty(value = "eureka.client.enabled", matchIfMissing = true)
		static class OnEurekaClient {

		}

	}

}
