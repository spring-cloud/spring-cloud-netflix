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

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServerInstanceProvider;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class EurekaConfigServerBootstrapperTests {

	@Test
	void notEnabledReturnsEmptyList() {
		new SpringApplicationBuilder(TestConfig.class)
				.properties("spring.cloud.service-registry.auto-registration.enabled=false")
				.addBootstrapRegistryInitializer(registry -> registry.addCloseListener(event -> {
					ConfigServerInstanceProvider.Function providerFn = event.getBootstrapContext()
							.get(ConfigServerInstanceProvider.Function.class);
					assertThat(providerFn.apply("id")).as("Should return empty list").isEmpty();
				})).run().close();
	}

	@Test
	public void discoveryClientNotEnabledProvidesEmptyList() {
		new SpringApplicationBuilder(TestConfig.class)
				.properties("spring.cloud.config.discovery.enabled=true", "spring.cloud.discovery.enabled=false",
						"spring.cloud.service-registry.auto-registration.enabled=false")
				.addBootstrapRegistryInitializer(registry -> registry.addCloseListener(event -> {
					ConfigServerInstanceProvider.Function providerFn = event.getBootstrapContext()
							.get(ConfigServerInstanceProvider.Function.class);
					Binder binder = event.getBootstrapContext().get(Binder.class);
					BindHandler bindHandler = event.getBootstrapContext().get(BindHandler.class);
					assertThat(providerFn.apply("id", binder, bindHandler, mock(Log.class)))
							.as("Should return empty list").isEmpty();
				})).run().close();
	}

	@Test
	public void eurekaClientNotEnabledProvidesEmptyList() {
		new SpringApplicationBuilder(TestConfig.class)
				.properties("spring.cloud.config.discovery.enabled=true", "eureka.client.enabled=false",
						"spring.cloud.service-registry.auto-registration.enabled=false")
				.addBootstrapRegistryInitializer(registry -> registry.addCloseListener(event -> {
					ConfigServerInstanceProvider.Function providerFn = event.getBootstrapContext()
							.get(ConfigServerInstanceProvider.Function.class);
					Binder binder = event.getBootstrapContext().get(Binder.class);
					BindHandler bindHandler = event.getBootstrapContext().get(BindHandler.class);
					assertThat(providerFn.apply("id", binder, bindHandler, mock(Log.class)))
							.as("Should return empty list").isEmpty();
				})).run().close();
	}

	@Test
	void enabledAddsInstanceProviderFn() {
		new SpringApplicationBuilder(TestConfig.class)
				.properties("spring.config.import: classpath:bootstrapper.yaml",
						ConfigClientProperties.PREFIX + ".enabled=true")
				.addBootstrapRegistryInitializer(registry -> registry.addCloseListener(event -> {
					ConfigServerInstanceProvider.Function providerFn = event.getBootstrapContext()
							.get(ConfigServerInstanceProvider.Function.class);
					Binder binder = event.getBootstrapContext().get(Binder.class);
					BindHandler bindHandler = event.getBootstrapContext().get(BindHandler.class);
					assertThatThrownBy(() -> providerFn.apply("id", binder, bindHandler, mock(Log.class)))
							.isInstanceOf(ResourceAccessException.class)
							.hasMessageContaining("I/O error on GET request for \"http://localhost:8761/eureka/apps/\"")
							.as("Should have tried to connect to Eureka to fetch instances.");
				})).run().close();
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	static class TestConfig {

	}

}
