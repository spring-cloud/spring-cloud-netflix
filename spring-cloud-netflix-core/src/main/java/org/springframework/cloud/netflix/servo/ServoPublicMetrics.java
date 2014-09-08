/*
 * Copyright 2013-2014 the original author or authors.
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

import java.util.Collection;

import javax.management.MBeanServer;

import org.springframework.boot.actuate.endpoint.VanillaPublicMetrics;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.reader.MetricReader;

/**
 * @author Dave Syer
 *
 */
public class ServoPublicMetrics extends VanillaPublicMetrics {

	private final ServoMetricReader servo;

	public ServoPublicMetrics(MetricReader reader, MBeanServer server) {
		super(reader);
		this.servo = new ServoMetricReader(server);
	}

	@Override
	public Collection<Metric<?>> metrics() {
		Collection<Metric<?>> metrics = super.metrics();
		if (servo != null) {
			for (Metric<?> metric : servo.findAll()) {
				metrics.add(metric);
			}
		}
		return metrics;
	}

}
