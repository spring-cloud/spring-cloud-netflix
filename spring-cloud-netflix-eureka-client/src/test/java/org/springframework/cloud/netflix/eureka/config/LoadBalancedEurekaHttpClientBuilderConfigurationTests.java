/*
 * Copyright 2017-present the original author or authors.
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

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.eureka.http.RestClientDiscoveryClientOptionalArgs;
import org.springframework.cloud.netflix.eureka.http.WebClientTransportClientFactories;
import org.springframework.cloud.netflix.eureka.sample.EurekaSampleApplication;
import org.springframework.cloud.test.ClassPathExclusions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author arimu1
 */
@ClassPathExclusions({ "jersey-client-*", "jersey-core-*", "jersey-apache-client4-*" })
class LoadBalancedEurekaHttpClientBuilderConfigurationTests {

	@Test
	void restClientTransportDoesNotUseLoadBalancedRestClientBuilderWhenItIsTheOnlyCandidate() {
		new WebApplicationContextRunner()
			.withUserConfiguration(EurekaSampleApplication.class, LoadBalancedRestClientConfiguration.class)
			.withPropertyValues("eureka.client.webclient.enabled=false")
			.run(context -> {
				RestClientDiscoveryClientOptionalArgs args = context
					.getBean(RestClientDiscoveryClientOptionalArgs.class);
				RestClient.Builder loadBalancedBuilder = context.getBean("loadBalancedRestClientBuilder",
						RestClient.Builder.class);
				Supplier<RestClient.Builder> supplier = getRestClientBuilderSupplier(args);
				assertThat(supplier.get()).isNotSameAs(loadBalancedBuilder);
			});
	}

	@Test
	void restClientTransportPrefersPlainRestClientBuilderOverLoadBalanced() {
		new WebApplicationContextRunner()
			.withUserConfiguration(EurekaSampleApplication.class, PlainAndLoadBalancedRestClientConfiguration.class)
			.withPropertyValues("eureka.client.webclient.enabled=false")
			.run(context -> {
				RestClientDiscoveryClientOptionalArgs args = context
					.getBean(RestClientDiscoveryClientOptionalArgs.class);
				RestClient.Builder plainBuilder = context.getBean("plainRestClientBuilder", RestClient.Builder.class);
				Supplier<RestClient.Builder> supplier = getRestClientBuilderSupplier(args);
				assertThat(supplier.get()).isSameAs(plainBuilder);
			});
	}

	@Test
	void webClientTransportDoesNotUseLoadBalancedWebClientBuilderWhenItIsTheOnlyCandidate() {
		new WebApplicationContextRunner()
			.withUserConfiguration(EurekaSampleApplication.class, LoadBalancedWebClientConfiguration.class)
			.withPropertyValues("eureka.client.webclient.enabled=true")
			.run(context -> {
				WebClientTransportClientFactories factories = context.getBean(WebClientTransportClientFactories.class);
				WebClient.Builder loadBalancedBuilder = context.getBean("loadBalancedWebClientBuilder",
						WebClient.Builder.class);
				Supplier<WebClient.Builder> supplier = getWebClientBuilderSupplier(factories);
				assertThat(supplier.get()).isNotSameAs(loadBalancedBuilder);
			});
	}

	@SuppressWarnings("unchecked")
	private static Supplier<RestClient.Builder> getRestClientBuilderSupplier(
			RestClientDiscoveryClientOptionalArgs args) {
		return (Supplier<RestClient.Builder>) ReflectionTestUtils.getField(args, "restClientBuilderSupplier");
	}

	@SuppressWarnings("unchecked")
	private static Supplier<WebClient.Builder> getWebClientBuilderSupplier(
			WebClientTransportClientFactories factories) {
		return (Supplier<WebClient.Builder>) ReflectionTestUtils.getField(factories, "builder");
	}

	@Configuration(proxyBeanMethods = false)
	static class LoadBalancedRestClientConfiguration {

		@Bean
		@LoadBalanced
		RestClient.Builder loadBalancedRestClientBuilder() {
			return RestClient.builder();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class PlainAndLoadBalancedRestClientConfiguration {

		@Bean
		RestClient.Builder plainRestClientBuilder() {
			return RestClient.builder();
		}

		@Bean
		@LoadBalanced
		RestClient.Builder loadBalancedRestClientBuilder() {
			return RestClient.builder();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class LoadBalancedWebClientConfiguration {

		@Bean
		@LoadBalanced
		WebClient.Builder loadBalancedWebClientBuilder() {
			return WebClient.builder();
		}

	}

}
