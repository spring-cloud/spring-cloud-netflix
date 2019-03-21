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

package org.springframework.cloud.netflix.hystrix;

import java.time.Duration;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class HystrixCommandsTests {

	@Test
	public void monoWorks() {
		StepVerifier.create(HystrixCommands.from(Flux.just("works"))
				.commandName("testworks").toMono()).expectNext("works").verifyComplete();
	}

	@Test
	public void eagerMonoWorks() {
		StepVerifier
				.create(HystrixCommands.from(Mono.just("works")).eager()
						.commandName("testworks").toMono())
				.expectNext("works").verifyComplete();
	}

	@Test
	public void monoTimesOut() {
		StepVerifier.create(HystrixCommands.from(Mono.fromCallable(() -> {
			Thread.sleep(1500);
			return "timeout";
		})).commandName("failcmd").toMono()).verifyError(HystrixRuntimeException.class);
	}

	@Test
	public void monoFallbackWorks() {
		StepVerifier
				.create(HystrixCommands.from(Mono.<String>error(new Exception()))
						.commandName("failcmd").fallback(Mono.just("fallback")).toMono())
				.expectNext("fallback").verifyComplete();
	}

	@Test
	public void monoFallbackWithExceptionWorks() {
		StepVerifier.create(
				HystrixCommands.from(Mono.<String>error(new IllegalStateException()))
						.commandName("failcmd").fallback(throwable -> {
							if (throwable instanceof IllegalStateException) {
								return Mono.just("specificfallback");
							}
							return Mono.just("genericfallback");
						}).toMono())
				.expectNext("specificfallback").verifyComplete();
	}

	@Test
	public void fluxWorks() {
		StepVerifier.create(HystrixCommands.from(Flux.just("1", "2"))
				.commandName("multiflux").toFlux()).expectNext("1").expectNext("2")
				.verifyComplete();
	}

	@Test
	public void fluxWorksDeferredRequest() {
		StepVerifier
				.create(HystrixCommands.from(Flux.just("1", "2")).commandName("multiflux")
						.build(), 1)
				.expectNext("1").thenAwait(Duration.ofSeconds(1)).thenRequest(1)
				.expectNext("2").verifyComplete();
	}

	@Test
	public void toObservableFunctionWorks() {
		StepVerifier
				.create(HystrixCommands.from(Flux.just("1", "2")).commandName("multiflux")
						.toObservable(cmd -> cmd.toObservable()).build(), 1)
				.expectNext("1").thenAwait(Duration.ofSeconds(1)).thenRequest(1)
				.verifyError();
	}

	@Test
	public void eagerFluxWorks() {
		StepVerifier
				.create(HystrixCommands.from(Flux.just("1", "2")).commandName("multiflux")
						.eager().toFlux())
				.expectNext("1").expectNext("2").verifyComplete();
	}

	@Test
	public void fluxTimesOut() {
		StepVerifier.create(HystrixCommands.from(Flux.from(s -> {
			try {
				Thread.sleep(1500);
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		})).commandName("failcmd").toFlux()).verifyError(HystrixRuntimeException.class);
	}

	@Test
	public void fluxFallbackWorks() {
		StepVerifier
				.create(HystrixCommands.from(Flux.<String>error(new Exception()))
						.commandName("multiflux").fallback(Flux.just("a", "b")).toFlux())
				.expectNext("a").expectNext("b").verifyComplete();
	}

	@Test
	public void extendTimeout() {
		StepVerifier.create(HystrixCommands.from(Mono.fromCallable(() -> {
			Thread.sleep(1500);
			return "works";
		})).commandName("extendTimeout")
				.commandProperties(
						setter -> setter.withExecutionTimeoutInMilliseconds(2000))
				.toMono()).expectNext("works").verifyComplete();
	}

}
