/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.hystrix;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.netflix.hystrix.exception.HystrixRuntimeException;

import static org.assertj.core.api.Assertions.assertThat;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class HystrixCommandsTests {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void monoWorks() {
		String result = HystrixCommands.wrap("testworks", Mono.just("works")).block();
		assertThat(result).isEqualTo("works");
	}

	@Test
	public void monoTimesOut() {
		exception.expect(HystrixRuntimeException.class);
		HystrixCommands.wrap("failcmd", Mono.fromCallable(() -> {
			Thread.sleep(1500);
			return "timeout";
		})).block();
	}

	@Test
	public void monoFallbackWorks() {
		String result = HystrixCommands.wrap("failcmd", Mono.error(new Exception()), Mono.just("fallback")).block();
		assertThat(result).isEqualTo("fallback");
	}

	@Test
	public void fluxWorks() {
		List<String> list = HystrixCommands.wrap("multiflux", Flux.just("1", "2")).collectList().block();
		assertThat(list).hasSize(2).contains("1", "2");
	}

	@Test
	// @Ignore
	public void fluxTimesOut() {
		exception.expect(HystrixRuntimeException.class);
		HystrixCommands.wrap("failcmd", Flux.from(s -> {
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		})).blockFirst();
	}

	@Test
	public void fluxFallbackWorks() {
		List<String> list = HystrixCommands.wrap("multiflux", Flux.error(new Exception()), Flux.just("a", "b")).collectList().block();
		assertThat(list).hasSize(2).contains("a", "b");
	}

}
