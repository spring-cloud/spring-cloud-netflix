/*
 * Copyright 2013-2024 the original author or authors.
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

import com.netflix.discovery.shared.transport.EurekaHttpClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.netflix.eureka.http.RestClientEurekaHttpClient;
import org.springframework.cloud.netflix.eureka.http.RestTemplateEurekaHttpClient;
import org.springframework.cloud.netflix.eureka.http.WebClientEurekaHttpClient;
import org.springframework.cloud.test.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Wonchul Heo
 */
class EurekaConfigServerBootstrapConfigurationClientTests {

	@Test
	void properBeansCreatedWhenRestTemplateEnabled() {
		new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(EurekaConfigServerBootstrapConfiguration.class))
			.withPropertyValues("spring.cloud.config.discovery.enabled=true")
			.withPropertyValues("eureka.client.enabled=true")
			.withPropertyValues("eureka.client.restclient.enabled=false")
			.run(context -> {
				assertThat(context).hasSingleBean(RestTemplateEurekaHttpClient.class);
				assertThat(context).doesNotHaveBean(RestClientEurekaHttpClient.class);
				assertThat(context).doesNotHaveBean(WebClientEurekaHttpClient.class);
			});
	}

	@Test
	void properBeansCreatedWhenRestClientEnabled() {
		new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(EurekaConfigServerBootstrapConfiguration.class))
			.withPropertyValues("spring.cloud.config.discovery.enabled=true")
			.withPropertyValues("eureka.client.enabled=true")
			.withPropertyValues("eureka.client.restclient.enabled=true")
			.run(context -> {
				assertThat(context).hasSingleBean(RestClientEurekaHttpClient.class);
				assertThat(context).doesNotHaveBean(WebClientEurekaHttpClient.class);
			});
	}

	@Test
	void properBeansCreatedWhenWebClientEnabled() {
		new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(EurekaConfigServerBootstrapConfiguration.class))
			.withPropertyValues("spring.cloud.config.discovery.enabled=true")
			.withPropertyValues("eureka.client.enabled=true")
			.withPropertyValues("eureka.client.webclient.enabled=true")
			.run(context -> {
				final EurekaHttpClient bean = context.getBean(EurekaHttpClient.class);
				System.out.println(bean);
				assertThat(context).hasSingleBean(WebClientEurekaHttpClient.class);
				assertThat(context).doesNotHaveBean(RestClientEurekaHttpClient.class);
			});
	}

	@Nested
	@ClassPathExclusions({ "spring-webflux-*" })
	static class NoWebFlux {

		@Test
		void properBeansCreatedWhenRestTemplateEnabled() {
			new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(EurekaConfigServerBootstrapConfiguration.class))
				.withPropertyValues("spring.cloud.config.discovery.enabled=true")
				.withPropertyValues("eureka.client.enabled=true")
				.withPropertyValues("eureka.client.restclient.enabled=false")
				.run(context -> {
					assertThat(context).hasSingleBean(RestTemplateEurekaHttpClient.class);
					assertThat(context).doesNotHaveBean(RestClientEurekaHttpClient.class);
					assertThat(context).doesNotHaveBean(WebClientEurekaHttpClient.class);
				});
		}

		@Test
		void properBeansCreatedWhenRestClientEnabled() {
			new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(EurekaConfigServerBootstrapConfiguration.class))
				.withPropertyValues("spring.cloud.config.discovery.enabled=true")
				.withPropertyValues("eureka.client.enabled=true")
				.withPropertyValues("eureka.client.restclient.enabled=true")
				.run(context -> {
					assertThat(context).hasSingleBean(RestClientEurekaHttpClient.class);
					assertThat(context).doesNotHaveBean(WebClientEurekaHttpClient.class);
				});
		}

		@Test
		void properBeansCreatedWhenWebClientEnabledThenFailed() {
			new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(EurekaConfigServerBootstrapConfiguration.class))
				.withPropertyValues("spring.cloud.config.discovery.enabled=true")
				.withPropertyValues("eureka.client.enabled=true")
				.withPropertyValues("eureka.client.webclient.enabled=true")
				.run(context -> {
					assertThatThrownBy(() -> context.getBean(WebClientEurekaHttpClient.class))
						.hasRootCauseInstanceOf(NoSuchBeanDefinitionException.class)
						.hasRootCauseMessage(
								"No qualifying bean of type 'com.netflix.discovery.shared.transport.EurekaHttpClient' available: "
										+ "expected at least 1 bean which qualifies as autowire candidate. "
										+ "Dependency annotations: {}");
				});
		}

	}

}
