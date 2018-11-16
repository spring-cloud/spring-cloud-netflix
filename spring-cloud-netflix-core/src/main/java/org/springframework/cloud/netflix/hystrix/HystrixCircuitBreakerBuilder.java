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

import org.springframework.cloud.client.circuitbreaker.CircuitBreakerBuilder;
import org.springframework.util.Assert;

/**
 * Builds Hystrix circuit breakers.
 *
 * @author Ryan Baxter
 */
public class HystrixCircuitBreakerBuilder implements CircuitBreakerBuilder<HystrixCircuitBreakerConfigFactory> {

	private String id;
	private HystrixCircuitBreakerConfigFactory configFactory;

	public HystrixCircuitBreakerBuilder() {
		this.configFactory = new HystrixCircuitBreakerConfigFactory.DefaultHystrixCircuitBreakerConfigFactory();
	}

	@Override
	public HystrixCircuitBreakerBuilder id(String id) {
		this.id = id;
		return this;
	}

	@Override
	public CircuitBreakerBuilder configFactory(HystrixCircuitBreakerConfigFactory configFactory) {
		this.configFactory = configFactory;
		return this;
	}

	@Override
	public HystrixCircuitBreaker build() {
		Assert.hasText(id, "A CircuitBreaker must have an id.");
		return new HystrixCircuitBreaker(id, configFactory.get(id));
	}

}
