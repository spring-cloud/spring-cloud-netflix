/*
 * Copyright 2013-2018 the original author or authors.
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

import java.time.Duration;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.cloud.netflix.test.NoSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Ryan Baxter
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT,
		classes = ReactiveHystrixCircuitBreakerIntegrationTest.Application.class)
@DirtiesContext
@Import(NoSecurityConfiguration.class)
public class ReactiveHystrixCircuitBreakerIntegrationTest {

	@LocalServerPort
	int port = 0;

	@Autowired
	ReactiveHystrixCircuitBreakerIntegrationTest.Application.DemoControllerService service;

	@Before
	public void setup() {
		service.setPort(port);
	}

	@Test
	public void testSlow() {
		assertThat(service.slow().block()).isEqualTo("fallback");
	}

	@Test
	public void testNormal() {
		assertThat(service.normal().block()).isEqualTo("normal");
	}

	@Configuration
	@EnableAutoConfiguration
	@RestController
	protected static class Application {

		@RequestMapping("/slow")
		public Mono<String> slow() {
			return Mono.just("slow").delayElement(Duration.ofSeconds(3));
		}

		@GetMapping("/normal")
		public Mono<String> normal() {
			return Mono.just("normal");
		}

		@Bean
		public Customizer<ReactiveHystrixCircuitBreakerFactory> customizer() {
			return factory -> factory
					.configure(
							builder -> builder.commandProperties(HystrixCommandProperties
									.Setter().withExecutionTimeoutInMilliseconds(2000)),
							"slow");
		}

		@Bean
		public Customizer<ReactiveHystrixCircuitBreakerFactory> defaultConfig() {
			return factory -> factory
					.configureDefault(id -> HystrixObservableCommand.Setter
							.withGroupKey(HystrixCommandGroupKey.Factory.asKey(id))
							.andCommandPropertiesDefaults(HystrixCommandProperties
									.Setter().withExecutionTimeoutInMilliseconds(4000)));
		}

		@Service
		public static class DemoControllerService {

			private int port = 0;

			private ReactiveCircuitBreakerFactory cbFactory;

			DemoControllerService(ReactiveCircuitBreakerFactory cbBuilder) {
				this.cbFactory = cbBuilder;
			}

			public Mono<String> slow() {
				return WebClient.builder().baseUrl("http://localhost:" + port).build()
						.get().uri("/slow").retrieve().bodyToMono(String.class)
						.transform(it -> cbFactory.create("slow").run(it, t -> {
							t.printStackTrace();
							return Mono.just("fallback");
						}));
			}

			public Mono<String> normal() {
				return WebClient.builder().baseUrl("http://localhost:" + port).build()
						.get().uri("/normal").retrieve().bodyToMono(String.class)
						.transform(it -> cbFactory.create("normal").run(it, t -> {
							t.printStackTrace();
							return Mono.just("fallback");
						}));
			}

			public void setPort(int port) {
				this.port = port;
			}

		}

	}

}
