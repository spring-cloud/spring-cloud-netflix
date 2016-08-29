/*
 * Copyright 2013-2014 the original author or authors.
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

import org.springframework.boot.actuate.autoconfigure.ExportMetricReader;
import org.springframework.boot.actuate.autoconfigure.MetricRepositoryAutoConfiguration;
import org.springframework.boot.actuate.endpoint.MetricReaderPublicMetrics;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.metrics.DefaultMetricsTagProvider;
import org.springframework.cloud.netflix.metrics.MetricsInterceptorConfiguration;
import org.springframework.cloud.netflix.metrics.MetricsTagProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.MonitorRegistry;
import com.netflix.servo.monitor.Monitors;

/**
 * Auto configuration to configure Servo support.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Jon Schneider
 */
@Configuration
@ConditionalOnClass({ Monitors.class, MetricReader.class })
@ConditionalOnMissingClass("com.netflix.spectator.api.Registry")
@AutoConfigureBefore(MetricRepositoryAutoConfiguration.class)
@Import(MetricsInterceptorConfiguration.class)
@ConditionalOnProperty(name = "spring.metrics.servo.enabled", matchIfMissing = true)
public class ServoMetricsAutoConfiguration {
	@Bean
	@ConditionalOnMissingBean
	public ServoMetricsConfigBean servoMetricsConfig() {
		return new ServoMetricsConfigBean();
	}

	@Bean
	@ConditionalOnMissingBean
	public ServoMetricNaming servoMetricNaming() {
		return new HierarchicalServoMetricNaming();
	}

	@Bean
	@ConditionalOnMissingBean
	public MonitorRegistry monitorRegistry(ServoMetricsConfigBean servoMetricsConfig) {
		System.setProperty(
				DefaultMonitorRegistry.class.getCanonicalName() + ".registryClass",
				servoMetricsConfig.getRegistryClass());
		return DefaultMonitorRegistry.getInstance();
	}

	@Bean
	public ServoMonitorCache monitorCache(MonitorRegistry monitorRegistry, ServoMetricsConfigBean servoMetricsConfig) {
		return new ServoMonitorCache(monitorRegistry, servoMetricsConfig);
	}

	@Bean
	@ExportMetricReader
	public ServoMetricReader servoMetricReader(MonitorRegistry monitorRegistry,
			ServoMetricNaming servoMetricNaming) {
		ServoMetricReader reader = new ServoMetricReader(monitorRegistry,
				servoMetricNaming);
		return reader;
	}

	@Bean
	public MetricReaderPublicMetrics servoPublicMetrics(ServoMetricReader reader) {
		return new MetricReaderPublicMetrics(reader);
	}

	@Bean
	@ConditionalOnMissingBean({ CounterService.class, GaugeService.class })
	public ServoMetricServices servoMetricServices(MonitorRegistry monitorRegistry) {
		return new ServoMetricServices(monitorRegistry);
	}

	@Configuration
	@ConditionalOnClass(name = "javax.servlet.http.HttpServletRequest")
	protected static class MetricsTagConfiguration {
		@Bean
		public MetricsTagProvider defaultMetricsTagProvider() {
			return new DefaultMetricsTagProvider();
		}
	}
}
