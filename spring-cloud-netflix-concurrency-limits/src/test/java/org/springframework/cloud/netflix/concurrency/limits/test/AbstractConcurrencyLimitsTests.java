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

package org.springframework.cloud.netflix.concurrency.limits.test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractConcurrencyLimitsTests {

	protected WebClient client;

	protected void assertLimiter(WebClient client) {
		// TODO: assert the body
		Flux<Tuple2<String, HttpStatus>> flux = Flux.range(1, 100)
				.flatMap(integer -> client.get().uri("/").exchange(), 4)
				// .log("reqs", Level.INFO)
				.flatMap(response -> response.bodyToMono(String.class).defaultIfEmpty("")
						/* .log("body2mono", Level.INFO) */
						.zipWith(Mono.just(response.statusCode())));

		Responses responses = new Responses();
		StepVerifier.create(flux).thenConsumeWhile(response -> true, response -> {
			HttpStatus status = response.getT2();
			if (status.equals(HttpStatus.OK)) {
				responses.success.incrementAndGet();
			}
			else if (status.equals(HttpStatus.TOO_MANY_REQUESTS)) {
				responses.tooManyReqs.incrementAndGet();
				String body = response.getT1();
				// TODO: body from handler isn't coming thru
				// assertThat(body).isEqualTo("Concurrency limit exceeded");
			}
			else {
				responses.other.incrementAndGet();
			}
		}).verifyComplete();

		System.out.println("Responses: " + responses);

		assertThat(responses.other).hasValue(0);
		assertThat(responses.tooManyReqs).hasValueGreaterThanOrEqualTo(1);

	}

	@Configuration
	@RestController
	protected static class HelloControllerConfiguration {

		@GetMapping
		public String get() {
			return "Hello";
		}

	}

}
