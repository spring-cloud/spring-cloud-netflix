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
 *
 */

package org.springframework.cloud.netflix.zuul.metrics;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.zuul.EnableZuulServer;
import org.springframework.cloud.netflix.zuul.util.ZuulRuntimeException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import com.netflix.zuul.exception.ZuulException;
import com.netflix.zuul.monitoring.CounterFactory;
import com.netflix.zuul.monitoring.TracerFactory;

import static org.junit.Assert.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
		ZuulMetricsApplicationTests.ZuulMetricsApplicationTestsConfiguration.class,
		ZuulMetricsApplicationTests.ZuulConfig.class }, webEnvironment = RANDOM_PORT)
@DirtiesContext
public class ZuulMetricsApplicationTests {

	@Autowired
	private CounterFactory counterFactory;
	@Autowired
	private TracerFactory tracerFactory;
	@Autowired
	private MeterRegistry meterRegistry;

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
	@SuppressWarnings("all")
	public void shouldIncrementCounters() throws Exception {
		new ZuulRuntimeException(new Exception());

		Double count = meterRegistry.counter("ZUUL::EXCEPTION:null:500").count();
		assertEquals(count.longValue(), 0L);

		new ZuulException("any", 500, "cause");
		new ZuulException("any", 500, "cause");

		count = meterRegistry.counter("ZUUL::EXCEPTION:cause:500").count();
		assertEquals(count.longValue(), 2L);

		new ZuulException("any", 404, "cause2");
		new ZuulException("any", 404, "cause2");
		new ZuulException("any", 404, "cause2");

		count = meterRegistry.counter("ZUUL::EXCEPTION:cause2:404").count();
		assertEquals(count.longValue(), 3L);
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
		public MeterRegistry meterRegistry() {
			return new SimpleMeterRegistry(SimpleConfig.DEFAULT, new MockClock());
		}
	}
}
