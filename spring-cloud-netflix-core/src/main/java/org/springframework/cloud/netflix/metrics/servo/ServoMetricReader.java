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

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.reader.MetricReader;

import com.netflix.servo.MonitorRegistry;
import com.netflix.servo.monitor.CompositeMonitor;
import com.netflix.servo.monitor.Monitor;

/**
 * @author Jon Schneider
 */
public class ServoMetricReader implements MetricReader {
	MonitorRegistry monitorRegistry;
	ServoMetricNaming metricNaming;

	public ServoMetricReader(MonitorRegistry monitorRegistry,
			ServoMetricNaming metricNaming) {
		this.monitorRegistry = monitorRegistry;
		this.metricNaming = metricNaming;
	}

	@Override
	public Metric<?> findOne(String s) {
		throw new UnsupportedOperationException(
				"cannot construct a tag-based Servo id from a hierarchical name");
	}

	@Override
	public Iterable<Metric<?>> findAll() {
		Collection<Metric<?>> metrics = new ArrayList<>();
		for (Monitor<?> monitor : monitorRegistry.getRegisteredMonitors()) {
			addToMetrics(monitor, metrics);
		}
		return metrics;
	}

	private void addToMetrics(Monitor<?> monitor, Collection<Metric<?>> metrics) {
		if (monitor instanceof CompositeMonitor) {
			for (Monitor<?> nestedMonitor : ((CompositeMonitor<?>) monitor).getMonitors()) {
				addToMetrics(nestedMonitor, metrics);
			}
		}
		else if (monitor.getValue() instanceof Number) {
			// Servo does support non-numeric values, but there is no such concept in
			// Spring Boot
			metrics.add(new Metric<>(metricNaming.asHierarchicalName(monitor),
					(Number) monitor.getValue()));
		}
	}

	@Override
	public long count() {
		long count = 0;
		for (Monitor<?> monitor : monitorRegistry.getRegisteredMonitors()) {
			count += countMetrics(monitor);
		}
		return count;
	}

	private static long countMetrics(Monitor<?> monitor) {
		if (monitor instanceof CompositeMonitor) {
			long count = 0;
			for (Monitor<?> nestedMonitor : ((CompositeMonitor<?>) monitor).getMonitors()) {
				count += countMetrics(nestedMonitor);
			}
			return count;
		}
		return 1;
	}
}
