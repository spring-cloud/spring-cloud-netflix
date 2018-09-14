/*
 * Copyright 2013-2018 the original author or authors.
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
 *
 */

package org.springframework.cloud.netflix.zuul.metrics;

import com.netflix.zuul.monitoring.CounterFactory;
import com.netflix.zuul.monitoring.TracerFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.zuul.EnableZuulServer;
import org.springframework.cloud.netflix.zuul.test.TestAutoConfiguration;
import org.springframework.cloud.test.ClassPathExclusions;
import org.springframework.cloud.test.ModifiedClassPathRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.assertEquals;

@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions({ "spring-boot-starter-actuator-*.jar",
		"spring-boot-actuator-*.jar", "micrometer-core-*.jar" })
public class ZuulEmptyMetricsApplicationTests {

	private ConfigurableApplicationContext context;

	@Before
	public void setUp() throws Exception {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(ZuulEmptyMetricsApplicationTestsConfiguration.class)
				.web(WebApplicationType.NONE)
				.run("--debug");
		this.context = context;
	}

	@After
	public void tearDown() throws Exception {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void shouldSetupDefaultCounterFactoryIfCounterServiceIsPresent()
			throws Exception {
		CounterFactory factory = this.context.getBean(CounterFactory.class);

		assertEquals(EmptyCounterFactory.class, factory.getClass());
	}

	@Test
	public void shouldSetupEmptyTracerFactory() throws Exception {
		TracerFactory factory = this.context.getBean(TracerFactory.class);

		assertEquals(EmptyTracerFactory.class, factory.getClass());
	}

	@EnableAutoConfiguration(exclude = TestAutoConfiguration.class)
	@Configuration
	// @Import(NoSecurityConfiguration.class)
	@EnableZuulServer
	@EnableConfigurationProperties(ServerProperties.class)
	static class ZuulEmptyMetricsApplicationTestsConfiguration {

	}
}
