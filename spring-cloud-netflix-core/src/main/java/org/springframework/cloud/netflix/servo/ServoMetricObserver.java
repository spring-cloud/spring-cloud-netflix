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

package org.springframework.cloud.netflix.servo;

import com.netflix.servo.Metric;
import com.netflix.servo.publish.BaseMetricObserver;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;

import java.util.Date;
import java.util.List;

/**
 * {@link com.netflix.servo.publish.MetricObserver} to convert Servo metrics into Spring Boot {@link org.springframework.boot.actuate.metrics.Metric}
 * instances.
 */
final class ServoMetricObserver extends BaseMetricObserver {

	private final MetricWriter metrics;
	private final ServoMetricNaming naming;

	public ServoMetricObserver(MetricWriter metrics, ServoMetricNaming naming) {
		super("spring-boot");
		this.metrics = metrics;
		this.naming = naming;
	}

	@Override
	public void updateImpl(List<Metric> servoMetrics) {
		for (Metric servoMetric : servoMetrics) {
			String key = naming.getName(servoMetric);

			if (servoMetric.hasNumberValue()) {
				this.metrics.set(new org.springframework.boot.actuate.metrics.Metric<>(key,
						servoMetric.getNumberValue(), new Date(servoMetric
								.getTimestamp())));
			}
		}
	}

}
