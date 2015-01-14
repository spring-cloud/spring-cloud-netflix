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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;

import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.publish.BaseMetricObserver;
import com.netflix.servo.publish.BasicMetricFilter;
import com.netflix.servo.publish.MetricObserver;
import com.netflix.servo.publish.MonitorRegistryMetricPoller;
import com.netflix.servo.publish.PollRunnable;
import com.netflix.servo.publish.PollScheduler;

/**
 * {@link MetricReader} implementation that registers a {@link MetricObserver} with the
 * Netflix Servo library and exposes Servo metrics to the <code>/metric</code> endpoint.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 */
@Slf4j
public class ServoMetricCollector implements DisposableBean {

	public ServoMetricCollector(MetricWriter metrics) {
		List<MetricObserver> observers = new ArrayList<MetricObserver>();
		observers.add(new ServoMetricObserver(metrics));
		PollRunnable task = new PollRunnable(new MonitorRegistryMetricPoller(),
				BasicMetricFilter.MATCH_ALL, true, observers);

		if (!PollScheduler.getInstance().isStarted()) {
			PollScheduler.getInstance().start();
		}
		// TODO Make poll interval configurable
		PollScheduler.getInstance().addPoller(task, 5, TimeUnit.SECONDS);
	}

	@Override
	public void destroy() throws Exception {
		log.info("Stopping Servo PollScheduler");
		if (PollScheduler.getInstance().isStarted()) {
			PollScheduler.getInstance().stop();
		}
	}

	/**
	 * {@link MetricObserver} to convert Servo metrics into Spring Boot {@link Metric}
	 * instances.
	 */
	private static final class ServoMetricObserver extends BaseMetricObserver {

		private final MetricWriter metrics;

		public ServoMetricObserver(MetricWriter metrics) {
			super("spring-boot");
			this.metrics = metrics;
		}

		@Override
		public void updateImpl(List<com.netflix.servo.Metric> servoMetrics) {
			for (com.netflix.servo.Metric servoMetric : servoMetrics) {
				MonitorConfig config = servoMetric.getConfig();
				String type = config.getTags().getValue("type");
				String key = new StringBuilder(type).append(".servo.")
						.append(config.getName()).toString().toLowerCase();

				if (servoMetric.hasNumberValue()) {
					this.metrics.set(new Metric<Number>(key,
							servoMetric.getNumberValue(), new Date(servoMetric
									.getTimestamp())));
				}
			}
		}
	}

}
