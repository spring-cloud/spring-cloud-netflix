/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.netflix.hystrix;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.test.ClassPathExclusions;
import org.springframework.cloud.test.ModifiedClassPathRunner;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.fail;

@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions({ "micrometer-core-*" })
public class HystrixAutoConfigurationTests {

	@Test
	@Ignore // TODO: why does this test fail in maven, but not in IDE?
	public void contextStarts() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder()
				.web(WebApplicationType.NONE).sources(TestApp.class).run()) {
			try {
				context.getBean("hystrixMetricsBinder");
				fail("HystrixMetricsBinder class should not be found");
			}
			catch (NoSuchBeanDefinitionException e) {
				// this is the correct case
			}
		}
	}

	@EnableCircuitBreaker
	@SpringBootConfiguration
	@EnableAutoConfiguration
	protected static class TestApp {

	}

}
