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

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.MonitorRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.EndpointAutoConfiguration;
import org.springframework.boot.actuate.endpoint.MetricReaderPublicMetrics;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.servo.publish.MetricPoller;
import com.netflix.servo.publish.MonitorRegistryMetricPoller;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.servo.ServoRegistry;

/**
 * Configures a basic Spectator registry that bridges to the legacy Servo API. We use this bridge because servo contains
 * an Atlas plugin that allows us to easily send all Spectator metrics to Atlas. Servo contains a similar plugin for
 * Graphite.
 *
 * Conditionally configures both an MVC interceptor and a RestTemplate interceptor that records metrics for request
 * handling timings.
 *
 * @author Jon Schneider
 */
@Configuration
@AutoConfigureBefore(EndpointAutoConfiguration.class)
public class SpectatorMetricsAutoConfiguration {
    @Value("${netflix.metrics.servo.registryClass:com.netflix.servo.BasicMonitorRegistry}")
    String servoRegistryClass;

    @Bean
    @ConditionalOnMissingBean
    public MonitorRegistry monitorRegistry() {
        System.setProperty(DefaultMonitorRegistry.class.getCanonicalName() + ".registryClass",
                servoRegistryClass);
        return DefaultMonitorRegistry.getInstance();
    }

	@Bean
	@ConditionalOnMissingBean(Registry.class)
	Registry registry(MonitorRegistry monitorRegistry) {
		return new ServoRegistry();
	}

	@Bean
	@ConditionalOnMissingBean(MetricPoller.class)
	MetricPoller metricPoller() {
		return new MonitorRegistryMetricPoller();
	}

	@Bean
	@ConditionalOnMissingBean
	public SpectatorMetricServices spectatorMetricServices(Registry metricRegistry) {
		return new SpectatorMetricServices(metricRegistry);
	}

	@Bean
	public MetricReaderPublicMetrics spectatorPublicMetrics(Registry metricRegistry) {
		SpectatorMetricReader reader = new SpectatorMetricReader(metricRegistry);
		return new MetricReaderPublicMetrics(reader);
	}
}
