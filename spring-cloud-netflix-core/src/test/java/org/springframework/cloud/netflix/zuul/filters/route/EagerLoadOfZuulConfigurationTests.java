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


package org.springframework.cloud.netflix.zuul.filters.route;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(value = { "zuul.routes.myroute.service-id=eager",
		"zuul.ribbon.eager-load.enabled=true" })
@DirtiesContext
public class EagerLoadOfZuulConfigurationTests {

	@Test
	public void testEagerLoading() {
		// Child context FooConfig should have been eagerly instantiated..
		assertThat(Foo.getInstanceCount()).isEqualTo(1);
	}

	@EnableAutoConfiguration
	@Configuration
	@EnableZuulProxy
	@RibbonClients(@RibbonClient(name = "eager", configuration = FooConfig.class))
	static class TestConfig {

	}

	static class Foo {
		private static final AtomicInteger INSTANCE_COUNT = new AtomicInteger();

		public Foo() {
			INSTANCE_COUNT.incrementAndGet();
		}

		public static int getInstanceCount() {
			return INSTANCE_COUNT.get();
		}
	}

	static class FooConfig {

		@Bean
		public Foo foo() {
			return new Foo();
		}

	}
}
