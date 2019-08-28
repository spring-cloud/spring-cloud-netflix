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

import java.util.function.Function;
import java.util.function.Supplier;

import com.netflix.hystrix.HystrixCommand;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;

/**
 * Hystrix implementation of {@link CircuitBreaker}.
 *
 * @author Ryan Baxter
 */
public class HystrixCircuitBreaker implements CircuitBreaker {

	private HystrixCommand.Setter setter;

	public HystrixCircuitBreaker(HystrixCommand.Setter setter) {
		this.setter = setter;
	}

	@Override
	public <T> T run(Supplier<T> toRun, Function<Throwable, T> fallback) {

		HystrixCommand<T> command = new HystrixCommand<T>(setter) {
			@Override
			protected T run() throws Exception {
				return toRun.get();
			}

			@Override
			protected T getFallback() {
				return fallback.apply(getExecutionException());
			}
		};
		return command.execute();
	}

}
