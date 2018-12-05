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
 */

package org.springframework.cloud.netflix.hystrix;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.assertj.core.util.Arrays;
import org.junit.Test;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;

import static org.junit.Assert.*;

/**
 * @author Ryan Baxter
 */
public class ReactiveHystrixCircuitBreakerTest {

	@Test
	public void monoRun() {
		HystrixCircuitBreakerConfigFactory configFactory = new HystrixCircuitBreakerConfigFactory.DefaultHystrixCircuitBreakerConfigFactory();
		ReactiveCircuitBreaker cb = new ReactiveHystrixCircuitBreakerFactory(configFactory).create("foo");
		Mono<String> s = cb.run(Mono.just("foobar"), t -> Mono.just("fallback"));
		assertEquals("foobar", s.block());
	}

	@Test
	public void monoFallback() {
		HystrixCircuitBreakerConfigFactory configFactory = new HystrixCircuitBreakerConfigFactory.DefaultHystrixCircuitBreakerConfigFactory();
		ReactiveCircuitBreaker cb = new ReactiveHystrixCircuitBreakerFactory(configFactory).create("foo");
		assertEquals("fallback", cb.run(Mono.error(new RuntimeException("boom")), t -> Mono.just("fallback")).block());
	}

	@Test
	public void fluxRun() {
		HystrixCircuitBreakerConfigFactory configFactory = new HystrixCircuitBreakerConfigFactory.DefaultHystrixCircuitBreakerConfigFactory();
		ReactiveCircuitBreaker cb = new ReactiveHystrixCircuitBreakerFactory(configFactory).create("foo");
		Flux<String> s = cb.run(Flux.just("foobar","hello world"), t -> Flux.just("fallback"));
		assertEquals(Arrays.asList(new String[]{"foobar", "hello world"}), s.collectList().block());
	}

	@Test
	public void fluxFallback() {
		HystrixCircuitBreakerConfigFactory configFactory = new HystrixCircuitBreakerConfigFactory.DefaultHystrixCircuitBreakerConfigFactory();
		ReactiveCircuitBreaker cb = new ReactiveHystrixCircuitBreakerFactory(configFactory).create("foo");
		assertEquals(Arrays.asList(new String[]{"fallback"}), cb.run(Flux.error(new RuntimeException("boom")), t -> Flux.just("fallback")).collectList().block());
	}
}