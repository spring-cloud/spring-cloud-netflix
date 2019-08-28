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

import org.assertj.core.util.Arrays;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ryan Baxter
 */
public class ReactiveHystrixCircuitBreakerTest {

	@Test
	public void monoRun() {
		ReactiveCircuitBreaker cb = new ReactiveHystrixCircuitBreakerFactory()
				.create("foo");
		Mono<String> s = Mono.just("foobar")
				.transform(it -> cb.run(it, t -> Mono.just("fallback")));
		assertThat(s.block()).isEqualTo("foobar");
	}

	@Test
	public void monoFallback() {
		ReactiveCircuitBreaker cb = new ReactiveHystrixCircuitBreakerFactory()
				.create("foo");
		assertThat(Mono.error(new RuntimeException("boom"))
				.transform(it -> cb.run(it, t -> Mono.just("fallback"))).block())
						.isEqualTo("fallback");
	}

	@Test
	public void fluxRun() {
		ReactiveCircuitBreaker cb = new ReactiveHystrixCircuitBreakerFactory()
				.create("foo");
		Flux<String> s = Flux.just("foobar", "hello world")
				.transform(it -> cb.run(it, t -> Flux.just("fallback")));
		assertThat(s.collectList().block())
				.isEqualTo(Arrays.asList(new String[] { "foobar", "hello world" }));
	}

	@Test
	public void fluxFallback() {
		ReactiveCircuitBreaker cb = new ReactiveHystrixCircuitBreakerFactory()
				.create("foo");
		assertThat(Flux.error(new RuntimeException("boom"))
				.transform(it -> cb.run(it, t -> Flux.just("fallback"))).collectList()
				.block()).isEqualTo(Arrays.asList(new String[] { "fallback" }));
	}

}
