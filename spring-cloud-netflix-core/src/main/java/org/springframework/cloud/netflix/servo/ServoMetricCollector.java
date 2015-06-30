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
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.extern.apachecommons.CommonsLog;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;

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
@CommonsLog
public class ServoMetricCollector implements DisposableBean {

	public ServoMetricCollector(MetricWriter metrics, ServoMetricNaming naming) {
		List<MetricObserver> observers = new ArrayList<>();
		observers.add(new ServoMetricObserver(metrics, naming));
		PollRunnable task = new PollRunnable(new MonitorRegistryMetricPoller(),
				BasicMetricFilter.MATCH_ALL, true, observers);

		if (!PollScheduler.getInstance().isStarted()) {
			try {
				PollScheduler.getInstance().start();
			}
			catch (Exception e) {
				// Can fail due to race condition with evil singletons (if more than one
				// app in same JVM)
				log.error("Could not start servo metrics poller", e);
				return;
			}
		}
		// TODO Make poll interval configurable
		PollScheduler.getInstance().addPoller(task, 5, TimeUnit.SECONDS);
	}

	@Override
	public void destroy() throws Exception {
		log.info("Stopping Servo PollScheduler");
		if (PollScheduler.getInstance().isStarted()) {
			try {
				PollScheduler.getInstance().stop();
			}
			catch (Exception e) {
				log.error("Could not stop servo metrics poller", e);
			}
		}
	}

}
