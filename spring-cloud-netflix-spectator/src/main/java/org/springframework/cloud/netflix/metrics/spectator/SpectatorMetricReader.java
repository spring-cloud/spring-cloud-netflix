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

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.reader.MetricReader;

import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Registry;

import static java.util.stream.StreamSupport.stream;

/**
 * Reads metrics from Spectator, placing Spectator tags in the form
 * <code>name(k1=v1,k2=v2)</code> where <code>name</code> is the metric name and the
 * contents of the parentheses are tag key-value pairs.
 *
 * @author Jon Schneider
 */
public class SpectatorMetricReader implements MetricReader {
	private Registry registry;

	public SpectatorMetricReader(Registry registry) {
		this.registry = registry;
	}

	protected static String asHierarchicalName(Id id) {
		List<String> tags = stream(id.tags().spliterator(), false).map(
				t -> t.key() + "=" + t.value()).collect(Collectors.toList());
		return id.name() + "(" + String.join(",", tags) + ")";
	}

	@Override
	public Metric<?> findOne(String name) {
		throw new UnsupportedOperationException(
				"cannot construct a tag-based Spectator id from a hierarchical name");
	}

	@Override
	public Iterable<Metric<?>> findAll() {
		return stream(registry.spliterator(), false)
				.flatMap(
						metric -> stream(metric.measure().spliterator(), false).map(
								measure -> new Metric<>(asHierarchicalName(measure.id()),
										measure.value())))
				.sorted((m1, m2) -> m1.getName().compareTo(m2.getName()))
				.collect(Collectors.toList());
	}

	@Override
	public long count() {
		return stream(registry.spliterator(), false).flatMap(
				m -> stream(m.measure().spliterator(), false)).count();
	}
}
