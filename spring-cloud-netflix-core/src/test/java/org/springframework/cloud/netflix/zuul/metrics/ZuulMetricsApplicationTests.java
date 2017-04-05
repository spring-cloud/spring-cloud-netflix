/*
 * Copyright 2013-2015 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.zuul.EnableZuulServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import com.netflix.zuul.exception.ZuulException;
import com.netflix.zuul.monitoring.CounterFactory;
import com.netflix.zuul.monitoring.TracerFactory;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
		ZuulMetricsApplicationTests.ZuulMetricsApplicationTestsConfiguration.class,
		ZuulMetricsApplicationTests.ZuulConfig.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class ZuulMetricsApplicationTests {

	private static final Map<String, Long> counters = new HashMap<>();

	@Autowired
	CounterFactory counterFactory;
	@Autowired
	TracerFactory tracerFactory;

	@Test
	public void shouldSetupDefaultCounterFactoryIfCounterServiceIsPresent()
			throws Exception {
		assertEquals(DefaultCounterFactory.class, counterFactory.getClass());
	}

	@Test
	public void shouldSetupEmptyTracerFactory() throws Exception {
		assertEquals(EmptyTracerFactory.class, tracerFactory.getClass());
	}

	@Test
	public void shouldIncrementCounters() throws Exception {
		new ZuulException("any", 500, "cause");
		new ZuulException("any", 500, "cause");

		assertEquals((long) counters.get("ZUUL::EXCEPTION:cause:500"), 2L);

		new ZuulException("any", 404, "cause2");
		new ZuulException("any", 404, "cause2");
		new ZuulException("any", 404, "cause2");

		assertEquals((long) counters.get("ZUUL::EXCEPTION:cause2:404"), 3L);
	}

	// Don't use @SpringBootApplication because we don't want to component scan
	@Configuration
	@EnableAutoConfiguration
	@EnableZuulServer
	static class ZuulConfig {

	}

	@Configuration
	static class ZuulMetricsApplicationTestsConfiguration {

		@Bean
		public ServerProperties serverProperties() {
			return new ServerProperties();
		}

		@Bean
		public CounterService counterService() {
			return new CounterService() {
				// not thread safe, but we are ok with it in tests
				@Override
				public void increment(String metricName) {
					Long counter = counters.get(metricName);
					if (counter == null) {
						counter = 0L;
					}
					counters.put(metricName, ++counter);
				}

				@Override
				public void decrement(String metricName) {
				}

				@Override
				public void reset(String metricName) {
				}
			};
		}
	}
}
