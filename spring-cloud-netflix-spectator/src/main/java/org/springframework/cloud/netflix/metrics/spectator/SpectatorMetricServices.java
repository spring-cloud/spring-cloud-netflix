/*
 * Copyright 2013-2015 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.cloud.netflix.metrics.spectator;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.GaugeService;

import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.impl.AtomicDouble;

/**
 * Provides a <code>CounterService</code> and <code>GaugeService</code> implementation
 * backed by Spectator.
 *
 * @author Jon Schneider
 */
public class SpectatorMetricServices implements CounterService, GaugeService {
	private final Registry registry;

	private final ConcurrentMap<Id, AtomicLong> counters = new ConcurrentHashMap<>();
	private final ConcurrentMap<Id, AtomicDouble> gauges = new ConcurrentHashMap<>();

	public SpectatorMetricServices(Registry registry) {
		this.registry = registry;
	}

	protected static String stripMetricName(String metricName) {
		return metricName.replaceFirst("^(timer|histogram|meter)\\.", "");
	}

	@Override
	public void increment(String name) {
		incrementInternal(name, 1L);
	}

	@Override
	public void decrement(String name) {
		incrementInternal(name, -1L);
	}

	private void incrementInternal(String name, long value) {
		if (name.startsWith("status.")) {
			// drop this metric since we are capturing it already with
			// SpectatorHandlerInterceptor,
			// and we are able to glean more information like exceptionType from that
			// mechanism than what
			// boot provides us
		}
		else if (name.startsWith("meter.")) {
			registry.counter(stripMetricName(name)).increment(value);
		}
		else {
			final Id id = registry.createId(name);
			final AtomicLong gauge = getCounterStorage(id);
			gauge.addAndGet(value);
		}
	}

	@Override
	public void reset(String name) {
		final Id id = registry.createId(stripMetricName(name));
		counters.remove(id);
		gauges.remove(id);
	}

	@Override
	public void submit(String name, double dValue) {
		if (name.startsWith("histogram.")) {
			registry.distributionSummary(stripMetricName(name)).record((long) dValue);
		}
		else if (name.startsWith("timer.")) {
			// Input is in milliseconds. Convert to nanos before casting to long to allow
			// sub-millisecond durations to be recorded correctly.
			long value = (long) (dValue * 1e6);
			registry.timer(stripMetricName(name)).record(value, TimeUnit.NANOSECONDS);
		}
		else {
			final Id id = registry.createId(name);
			final AtomicDouble gauge = getGaugeStorage(id);
			gauge.set(dValue);
		}
	}

	private AtomicDouble getGaugeStorage(Id id) {
		final AtomicDouble newGauge = new AtomicDouble(0);
		final AtomicDouble existingGauge = gauges.putIfAbsent(id, newGauge);
		return existingGauge == null ? registry.gauge(id, newGauge) : existingGauge;
	}

	private AtomicLong getCounterStorage(Id id) {
		final AtomicLong newCounter = new AtomicLong(0);
		final AtomicLong existingCounter = counters.putIfAbsent(id, newCounter);
		return existingCounter == null ? registry.gauge(id, newCounter) : existingCounter;
	}
}
