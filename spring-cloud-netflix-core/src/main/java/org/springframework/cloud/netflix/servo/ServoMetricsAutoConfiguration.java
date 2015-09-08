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

import org.springframework.boot.actuate.autoconfigure.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.MetricRepositoryAutoConfiguration;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.actuate.metrics.writer.Delta;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.servo.monitor.Monitors;

/**
 * Auto configuration to configure Servo support.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 */
@Configuration
@ConditionalOnClass({ Monitors.class, MetricReader.class })
@ConditionalOnBean(GaugeService.class)
@AutoConfigureBefore(EndpointAutoConfiguration.class)
@AutoConfigureAfter({ MetricRepositoryAutoConfiguration.class })
@Deprecated
public class ServoMetricsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public ServoMetricNaming servoMetricNaming() {
		return new DefaultServoMetricNaming();
	}

	@Bean
	@ConditionalOnMissingBean
	public ServoMetricCollector servoMetricCollector(GaugeService gauges,
			ServoMetricNaming naming) {
		return new ServoMetricCollector(new ActuatorMetricWriter(gauges), naming);
	}

	// TODO: refactor this when Spring Boot 1.3 is mandatory
	private static class ActuatorMetricWriter implements MetricWriter {

		private GaugeService gauges;

		public ActuatorMetricWriter(GaugeService gauges) {
			this.gauges = gauges;
		}

		@Override
		public void increment(Delta<?> delta) {
		}

		@Override
		public void set(Metric<?> value) {
			gauges.submit(value.getName(), value.getValue().doubleValue());
		}

		@Override
		public void reset(String metricName) {
			gauges.submit(metricName, 0.);
		}

	}

}
