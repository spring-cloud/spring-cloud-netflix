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

package org.springframework.cloud.netflix.ribbon;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Biju Kunjummen
 */

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {RibbonAutoConfiguration.class,
		ArchaiusAutoConfiguration.class, RibbonApplicationContextInitializerTests.RibbonInitializerConfig.class})
@DirtiesContext
public class RibbonApplicationContextInitializerTests {

	@Autowired
	private SpringClientFactory springClientFactory;

	@Test
	public void testContextShouldInitalizeChildContexts() {

		// Context should have been initialized and an instance of Foo created
		assertThat(Foo.getInstanceCount()).isEqualTo(1);
		ApplicationContext ctx = springClientFactory.getContext("testspec");

		assertThat(Foo.getInstanceCount()).isEqualTo(1);
		Foo foo = ctx.getBean("foo", Foo.class);
		assertThat(foo).isNotNull();
	}

	static class FooConfig {

		@Bean
		public Foo foo() {
			return new Foo();
		}

	}

	@Configuration
	@RibbonClient(name="testspec", configuration = FooConfig.class)
	static class RibbonInitializerConfig {

		@Bean
		public RibbonApplicationContextInitializer ribbonApplicationContextInitializer(
				SpringClientFactory springClientFactory) {
			return new RibbonApplicationContextInitializer(springClientFactory,
					Arrays.asList("testspec"));
		}

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
}
