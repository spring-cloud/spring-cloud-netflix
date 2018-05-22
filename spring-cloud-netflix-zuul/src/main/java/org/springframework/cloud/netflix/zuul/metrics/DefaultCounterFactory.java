/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.cloud.netflix.zuul.metrics;

import com.netflix.zuul.monitoring.CounterFactory;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * A counter based monitoring factory that uses {@link MeterRegistry} to increment counters.
 *
 * @author Anastasiia Smirnova
 */
public class DefaultCounterFactory extends CounterFactory {

	private final MeterRegistry meterRegistry;

	public DefaultCounterFactory(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	@Override
	public void increment(String name) {
		this.meterRegistry.counter(name).increment();
	}
}
