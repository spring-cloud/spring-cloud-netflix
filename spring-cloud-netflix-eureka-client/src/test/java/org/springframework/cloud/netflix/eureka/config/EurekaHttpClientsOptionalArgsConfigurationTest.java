/*
 * Copyright 2017-2020 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cloud.netflix.eureka.http.RestTemplateDiscoveryClientOptionalArgs;
import org.springframework.cloud.netflix.eureka.http.WebClientDiscoveryClientOptionalArgs;
import org.springframework.cloud.netflix.eureka.sample.EurekaSampleApplication;
import org.springframework.cloud.test.ClassPathExclusions;
import org.springframework.cloud.test.ModifiedClassPathRunner;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * @author Daniel Lavoie
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions({ "jersey-client-*", "jersey-core-*", "jersey-apache-client4-*" })
@SpringBootTest(classes = EurekaSampleApplication.class,
		webEnvironment = WebEnvironment.RANDOM_PORT)
public class EurekaHttpClientsOptionalArgsConfigurationTest {

	@Test
	public void contextLoadsWithRestTemplate() {
		new WebApplicationContextRunner()
				.withUserConfiguration(EurekaSampleApplication.class)
				.withPropertyValues("eureka.client.webclient.enabled=false")
				.run(context -> {
					assertThat(context)
							.hasSingleBean(RestTemplateDiscoveryClientOptionalArgs.class);
					assertThat(context)
							.doesNotHaveBean(WebClientDiscoveryClientOptionalArgs.class);
				});
	}

	@Test
	public void contextLoadsWithWebClient() {
		new WebApplicationContextRunner()
				.withUserConfiguration(EurekaSampleApplication.class)
				.withPropertyValues("eureka.client.webclient.enabled=true")
				.run(context -> {
					assertThat(context).doesNotHaveBean(
							RestTemplateDiscoveryClientOptionalArgs.class);
					assertThat(context)
							.hasSingleBean(WebClientDiscoveryClientOptionalArgs.class);
				});
	}

	@Test
	public void contextLoadsWithRestTemplateAsDefault() {
		new WebApplicationContextRunner()
				.withUserConfiguration(EurekaSampleApplication.class).run(context -> {
					assertThat(context)
							.hasSingleBean(RestTemplateDiscoveryClientOptionalArgs.class);
					assertThat(context)
							.doesNotHaveBean(WebClientDiscoveryClientOptionalArgs.class);
				});
	}

}
