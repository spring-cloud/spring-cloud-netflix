/*
 * Copyright 2019-2019 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.reactive;

import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.client.ConditionalOnDiscoveryHealthIndicatorEnabled;
import org.springframework.cloud.client.ConditionalOnReactiveDiscoveryEnabled;
import org.springframework.cloud.client.ReactiveCommonsClientAutoConfiguration;
import org.springframework.cloud.client.discovery.composite.reactive.ReactiveCompositeDiscoveryClientAutoConfiguration;
import org.springframework.cloud.client.discovery.health.DiscoveryClientHealthIndicatorProperties;
import org.springframework.cloud.client.discovery.health.reactive.ReactiveDiscoveryClientHealthIndicator;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration related to service discovery when using Netflix Eureka.
 *
 * @author Tim Ysewyn
 */
@Configuration
@ConditionalOnClass(EurekaClientConfig.class)
@ConditionalOnDiscoveryEnabled
@ConditionalOnReactiveDiscoveryEnabled
@ConditionalOnProperty(value = "eureka.client.enabled", matchIfMissing = true)
@EnableConfigurationProperties
@AutoConfigureAfter({ EurekaClientAutoConfiguration.class,
		ReactiveCompositeDiscoveryClientAutoConfiguration.class })
@AutoConfigureBefore(ReactiveCommonsClientAutoConfiguration.class)
@ImportAutoConfiguration(EurekaClientAutoConfiguration.class)
public class EurekaReactiveDiscoveryClientConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public EurekaReactiveDiscoveryClient eurekaReactiveDiscoveryClient(
			EurekaClient client, EurekaClientConfig clientConfig) {
		return new EurekaReactiveDiscoveryClient(client, clientConfig);
	}

	@Bean
	@ConditionalOnClass(
			name = "org.springframework.boot.actuate.health.ReactiveHealthIndicator")
	@ConditionalOnDiscoveryHealthIndicatorEnabled
	public ReactiveDiscoveryClientHealthIndicator eurekaReactiveDiscoveryClientHealthIndicator(
			EurekaReactiveDiscoveryClient client,
			DiscoveryClientHealthIndicatorProperties properties) {
		return new ReactiveDiscoveryClientHealthIndicator(client, properties);
	}

}
