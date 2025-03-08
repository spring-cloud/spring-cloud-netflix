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

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cloud.netflix.eureka.http.RestTemplateDiscoveryClientOptionalArgs;
import org.springframework.cloud.netflix.eureka.http.WebClientDiscoveryClientOptionalArgs;
import org.springframework.cloud.netflix.eureka.sample.EurekaSampleApplication;
import org.springframework.cloud.test.ClassPathExclusions;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * @author Daniel Lavoie
 */
@ClassPathExclusions({ "jersey-client-*", "jersey-core-*", "jersey-apache-client4-*" })
class EurekaHttpClientsOptionalArgsConfigurationTests {

	@Test
	void contextLoadsWithRestTemplate() {
		new WebApplicationContextRunner().withUserConfiguration(EurekaSampleApplication.class)
			.withPropertyValues("eureka.client.webclient.enabled=false")
			.run(context -> {
				assertThat(context).hasSingleBean(RestTemplateDiscoveryClientOptionalArgs.class);
				assertThat(context).doesNotHaveBean(WebClientDiscoveryClientOptionalArgs.class);
			});
	}

	@Test
	void contextLoadsWithWebClient() {
		new WebApplicationContextRunner().withUserConfiguration(EurekaSampleApplication.class)
			.withPropertyValues("eureka.client.webclient.enabled=true")
			.run(context -> {
				assertThat(context).doesNotHaveBean(RestTemplateDiscoveryClientOptionalArgs.class);
				assertThat(context).hasSingleBean(WebClientDiscoveryClientOptionalArgs.class);
			});
	}

	@Test
	void contextLoadsWithRestTemplateAsDefault() {
		new WebApplicationContextRunner().withUserConfiguration(EurekaSampleApplication.class).run(context -> {
			assertThat(context).hasSingleBean(RestTemplateDiscoveryClientOptionalArgs.class);
			assertThat(context).doesNotHaveBean(WebClientDiscoveryClientOptionalArgs.class);
		});
	}

}
