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

import org.springframework.boot.actuate.autoconfigure.MetricRepositoryAutoConfiguration;
import org.springframework.boot.actuate.endpoint.MetricReaderPublicMetrics;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.netflix.metrics.DefaultMetricsTagProvider;
import org.springframework.cloud.netflix.metrics.MetricsInterceptorConfiguration;
import org.springframework.cloud.netflix.metrics.MetricsTagProvider;
import org.springframework.cloud.netflix.metrics.servo.ServoMetricsConfigBean;
import org.springframework.cloud.netflix.metrics.servo.ServoMonitorCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.MonitorRegistry;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.servo.ServoRegistry;

/**
 * Configures a basic Spectator registry that bridges to the legacy Servo API. We use this
 * bridge because servo contains an Atlas plugin that allows us to easily send all
 * Spectator metrics to Atlas. Servo contains a similar plugin for Graphite.
 *
 * Conditionally configures both an MVC interceptor and a RestTemplate interceptor that
 * records metrics for request handling timings.
 *
 * @author Jon Schneider
 */
@Configuration
@AutoConfigureBefore(MetricRepositoryAutoConfiguration.class)
@ConditionalOnClass({ Registry.class, MetricReader.class })
@Import(MetricsInterceptorConfiguration.class)
public class SpectatorMetricsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public ServoMetricsConfigBean servoMetricsConfig() {
		return new ServoMetricsConfigBean();
	}

	@Bean
	@ConditionalOnMissingBean
	public MonitorRegistry monitorRegistry(ServoMetricsConfigBean configBean) {
		System.setProperty(DefaultMonitorRegistry.class.getCanonicalName()
				+ ".registryClass", configBean.getRegistryClass());
		return DefaultMonitorRegistry.getInstance();
	}

	@Bean
	@ConditionalOnMissingBean(Registry.class)
	Registry registry(MonitorRegistry monitorRegistry) {
		return new ServoRegistry();
	}

	@Bean
	public ServoMonitorCache monitorCache(MonitorRegistry monitorRegistry, ServoMetricsConfigBean configBean) {
		return new ServoMonitorCache(monitorRegistry, configBean);
	}

	@Bean
	@ConditionalOnMissingBean({ CounterService.class, GaugeService.class })
	public SpectatorMetricServices spectatorMetricServices(Registry metricRegistry) {
		return new SpectatorMetricServices(metricRegistry);
	}

	@Bean
	public MetricReaderPublicMetrics spectatorPublicMetrics(Registry metricRegistry) {
		SpectatorMetricReader reader = new SpectatorMetricReader(metricRegistry);
		return new MetricReaderPublicMetrics(reader);
	}

	@Bean
	public MetricsTagProvider defaultMetricsTagProvider() {
		return new DefaultMetricsTagProvider();
	}
}
