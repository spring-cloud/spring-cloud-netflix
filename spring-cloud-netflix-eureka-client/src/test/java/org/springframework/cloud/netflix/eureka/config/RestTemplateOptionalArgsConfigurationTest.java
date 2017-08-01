/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.eureka.config;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.ClassPathExclusions;
import org.springframework.cloud.FilteredClassPathRunner;
import org.springframework.cloud.netflix.eureka.http.RestTemplateDiscoveryClientOptionalArgs;
import org.springframework.cloud.netflix.eureka.sample.EurekaSampleApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author Daniel Lavoie
 */
@RunWith(FilteredClassPathRunner.class)
@ClassPathExclusions({ "jersey-client-*", "jersey-core-*", "jersey-apache-client4-*" })
@SpringBootTest(classes = EurekaSampleApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class RestTemplateOptionalArgsConfigurationTest {
	@Test
	public void contextLoads() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder()
				.web(false).sources(EurekaSampleApplication.class).run()) {
			Assert.assertNotNull(
					context.getBean(RestTemplateDiscoveryClientOptionalArgs.class));
		}
	}
}
