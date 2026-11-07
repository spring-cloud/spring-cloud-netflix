/*
 * Copyright 2013-present the original author or authors.
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

package org.springframework.cloud.netflix.eureka;

import java.util.Collections;
import java.util.List;

import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EurekaDiscoveryClientConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(EurekaDiscoveryClientConfiguration.class))
		.withBean(EurekaClient.class, () -> mock(EurekaClient.class))
		.withBean(EurekaClientConfig.class, () -> mock(EurekaClientConfig.class));

	@Test
	void shouldCreateEurekaDiscoveryClientByDefault() {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(DiscoveryClient.class);
			assertThat(context).hasSingleBean(EurekaDiscoveryClient.class);
		});
	}

	@Test
	void shouldBackOffWhenCustomDiscoveryClientExists() {
		this.contextRunner.withUserConfiguration(CustomDiscoveryClientConfiguration.class).run(context -> {
			assertThat(context).hasSingleBean(DiscoveryClient.class);
			assertThat(context).hasSingleBean(CustomDiscoveryClient.class);
			assertThat(context).doesNotHaveBean(EurekaDiscoveryClient.class);
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomDiscoveryClientConfiguration {

		@Bean
		DiscoveryClient customDiscoveryClient() {
			return new CustomDiscoveryClient();
		}

	}

	static class CustomDiscoveryClient implements DiscoveryClient {

		@Override
		public String description() {
			return "Custom Discovery Client";
		}

		@Override
		public List<ServiceInstance> getInstances(String serviceId) {
			return Collections.emptyList();
		}

		@Override
		public List<String> getServices() {
			return Collections.emptyList();
		}

	}

}
