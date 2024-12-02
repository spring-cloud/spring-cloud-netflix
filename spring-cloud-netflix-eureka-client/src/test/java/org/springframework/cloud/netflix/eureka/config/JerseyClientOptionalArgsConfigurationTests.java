/*
 * Copyright 2017-2024 the original author or authors.
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

import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.netflix.eureka.http.RestTemplateDiscoveryClientOptionalArgs;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Jersey client setup in DiscoveryClientOptionalArgsConfiguration.
 *
 * @author Olga Maciaszek-Sharma
 */
@SuppressWarnings("deprecation")
public class JerseyClientOptionalArgsConfigurationTests {

	@SuppressWarnings("OptionalGetWithoutIsPresent")
	@Test
	void shouldCreateRestTemplateDiscoveryClientOptionalArgsWhenJerseyClientDisabled() {
		new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(DiscoveryClientOptionalArgsConfiguration.class))
			.withPropertyValues("eureka.client.jersey.enabled=false")
			.run(context -> {
				assertThat(context).hasSingleBean(AbstractDiscoveryClientOptionalArgs.class);
				assertThat(context.getBeansOfType(AbstractDiscoveryClientOptionalArgs.class)
					.values()
					.stream()
					.findFirst()
					.get()).isInstanceOf(RestTemplateDiscoveryClientOptionalArgs.class);
				assertThat(context).hasSingleBean(RestTemplateDiscoveryClientOptionalArgs.class);
			});
	}

}
