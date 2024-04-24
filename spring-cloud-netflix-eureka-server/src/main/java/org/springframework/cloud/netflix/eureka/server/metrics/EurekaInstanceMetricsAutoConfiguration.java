/*
 * Copyright 2013-2024 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.server.metrics;

import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.eureka.server.EurekaServerAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Eureka Instance metrics.
 *
 * @author Wonchul Heo
 * @since 4.1.2
 */
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnBean(MeterRegistry.class)
@AutoConfigureAfter({ MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class,
		EurekaServerAutoConfiguration.class })
@ConditionalOnProperty(name = "eureka.server.metrics.enabled", havingValue = "true")
class EurekaInstanceMetricsAutoConfiguration {

	@ConditionalOnMissingBean
	@Bean
	public EurekaInstanceTagsProvider eurekaInstanceTagProvider() {
		return new DefaultEurekaInstanceTagsProvider();
	}

	@ConditionalOnMissingBean
	@Bean
	public EurekaInstanceMonitor eurekaInstanceMeterBinder(MeterRegistry meterRegistry,
			PeerAwareInstanceRegistry instanceRegistry, EurekaInstanceTagsProvider tagProvider) {
		return new EurekaInstanceMonitor(meterRegistry, instanceRegistry, tagProvider);
	}

}
