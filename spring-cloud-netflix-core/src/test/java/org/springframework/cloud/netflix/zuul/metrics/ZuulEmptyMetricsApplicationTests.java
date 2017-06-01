/*
 * Copyright 2013-2017 the original author or authors.
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

package org.springframework.cloud.netflix.zuul.metrics;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.cloud.ClassPathExclusions;
import org.springframework.cloud.netflix.zuul.ZuulServerAutoConfiguration;
import org.springframework.cloud.netflix.zuul.ZuulServerMarkerConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.zuul.monitoring.CounterFactory;
import com.netflix.zuul.monitoring.TracerFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@ClassPathExclusions({ "spring-boot-starter-actuator-*.jar",
		"spring-boot-actuator-*.jar" })
public class ZuulEmptyMetricsApplicationTests {

	private AnnotationConfigApplicationContext context;

	@Before
	public void setUp() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(ZuulEmptyMetricsApplicationTestsConfiguration.class,
			ZuulServerMarkerConfiguration.class, ZuulServerAutoConfiguration.class);
		context.refresh();

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

	@Configuration
	static class ZuulEmptyMetricsApplicationTestsConfiguration {

		@Bean
		ServerProperties serverProperties() {
			return new ServerProperties();
		}

	}
}
