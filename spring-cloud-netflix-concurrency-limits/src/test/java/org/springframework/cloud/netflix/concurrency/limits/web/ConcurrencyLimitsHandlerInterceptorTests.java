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

package org.springframework.cloud.netflix.concurrency.limits.web;

import java.util.function.Consumer;

import com.netflix.concurrency.limits.limit.FixedLimit;
import com.netflix.concurrency.limits.servlet.ServletLimiterBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.netflix.concurrency.limits.test.AbstractConcurrencyLimitsTests;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "logging.level.reactor.netty=DEBUG",
		webEnvironment = RANDOM_PORT)
public class ConcurrencyLimitsHandlerInterceptorTests
		extends AbstractConcurrencyLimitsTests {

	@LocalServerPort
	public int port;

	@Before
	public void init() {
		client = WebClient.create("http://localhost:" + port);
	}

	@Test
	public void handlerInterceptorWorks() {
		assertLimiter(client);
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@Import(HelloControllerConfiguration.class)
	protected static class TestConfig {

		@Bean
		public Consumer<ServletLimiterBuilder> limiterBuilderConfigurer() {
			return servletLimiterBuilder -> servletLimiterBuilder.limit(FixedLimit.of(1));
		}

	}

}
