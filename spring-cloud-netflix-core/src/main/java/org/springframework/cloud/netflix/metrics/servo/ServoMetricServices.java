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

package org.springframework.cloud.netflix.metrics.servo;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.GaugeService;

import com.netflix.servo.MonitorRegistry;
import com.netflix.servo.monitor.BasicCounter;
import com.netflix.servo.monitor.BasicDistributionSummary;
import com.netflix.servo.monitor.BasicTimer;
import com.netflix.servo.monitor.DoubleGauge;
import com.netflix.servo.monitor.LongGauge;
import com.netflix.servo.monitor.MonitorConfig;

/**
 * Provides a <code>CounterService</code> and <code>GaugeService</code> implementation
 * backed by Servo.
 *
 * @author Jon Schneider
 */
public class ServoMetricServices implements CounterService, GaugeService {
	private final MonitorRegistry registry;

	private final ConcurrentMap<String, BasicCounter> counters = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, LongGauge> longGauges = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, DoubleGauge> doubleGauges = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, BasicDistributionSummary> distributionSummaries = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, BasicTimer> timers = new ConcurrentHashMap<>();

	public ServoMetricServices(MonitorRegistry registry) {
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
		String strippedName = stripMetricName(name);

		if (name.startsWith("status.")) {
			// drop this metric since we are capturing it already with
			// ServoHandlerInterceptor,
			// and we are able to glean more information like exceptionType from that
			// mechanism than what
			// boot provides us
		}
		else if (name.startsWith("meter.")) {
			BasicCounter counter = counters.get(strippedName);
			if (counter == null) {
				counter = new BasicCounter(MonitorConfig.builder(strippedName).build());
				counters.put(strippedName, counter);
				registry.register(counter);
			}
			counter.increment(value);
		}
		else {
			LongGauge gauge = longGauges.get(strippedName);
			if (gauge == null) {
				gauge = new LongGauge(MonitorConfig.builder(strippedName).build());
				longGauges.put(strippedName, gauge);
				registry.register(gauge);
			}
			gauge.set(value);
		}
	}

	@Override
	public void reset(String name) {
		String strippedName = stripMetricName(name);
		BasicCounter counter = counters.remove(strippedName);
		if (counter != null)
			registry.unregister(counter);

		LongGauge gauge = longGauges.remove(strippedName);
		if (gauge != null)
			registry.unregister(gauge);

		BasicDistributionSummary distributionSummary = distributionSummaries
				.remove(strippedName);
		if (distributionSummary != null)
			registry.unregister(distributionSummary);
	}

	@Override
	public void submit(String name, double dValue) {
		long value = ((Double) dValue).longValue();
		String strippedName = stripMetricName(name);
		if (name.startsWith("histogram.")) {
			BasicDistributionSummary distributionSummary = distributionSummaries
					.get(strippedName);
			if (distributionSummary == null) {
				distributionSummary = new BasicDistributionSummary(MonitorConfig.builder(
						strippedName).build());
				distributionSummaries.put(strippedName, distributionSummary);
				registry.register(distributionSummary);
			}
			distributionSummary.record(value);
		}
		else if (name.startsWith("timer.")) {
			BasicTimer timer = timers.get(strippedName);
			if (timer == null) {
				timer = new BasicTimer(MonitorConfig.builder(strippedName).build());
				timers.put(strippedName, timer);
				registry.register(timer);
			}
			timer.record(value, TimeUnit.MILLISECONDS);
		}
		else {
			DoubleGauge gauge = doubleGauges.get(strippedName);
			if (gauge == null) {
				gauge = new DoubleGauge(MonitorConfig.builder(strippedName).build());
				doubleGauges.put(strippedName, gauge);
				registry.register(gauge);
			}
			gauge.set(dValue);
		}
	}
}
