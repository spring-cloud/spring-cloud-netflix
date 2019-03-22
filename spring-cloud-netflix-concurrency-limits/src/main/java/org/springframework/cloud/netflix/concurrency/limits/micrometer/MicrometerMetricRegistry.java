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

package org.springframework.cloud.netflix.concurrency.limits.micrometer;

import java.util.function.Supplier;

import com.netflix.concurrency.limits.MetricRegistry;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

/**
 * A Micrometer-specific {@link MetricRegistry} implementation.
 *
 * @author Spencer Gibb
 */
public class MicrometerMetricRegistry implements MetricRegistry {

	private final MeterRegistry meterRegistry;

	// TODO: baseId?

	public MicrometerMetricRegistry(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	@Override
	public SampleListener registerDistribution(String id, String... tagNameValuePairs) {
		DistributionSummary summary = this.meterRegistry.summary(id, tagNameValuePairs);
		return value -> summary.record(value.longValue());
	}

	@Override
	public void registerGauge(String id, Supplier<Number> supplier,
			String... tagNameValuePairs) {
		this.meterRegistry.gauge(id, Tags.of(tagNameValuePairs), supplier.get());
	}

}
