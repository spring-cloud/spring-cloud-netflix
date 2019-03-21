/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.cloud.netflix.turbine;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.discovery.EurekaClient;
import com.netflix.turbine.discovery.InstanceDiscovery;
import com.netflix.turbine.streaming.servlet.TurbineStreamServlet;

/**
 * @author Spencer Gibb
 */
@Configuration
@EnableConfigurationProperties
@EnableDiscoveryClient
public class TurbineHttpConfiguration {

	@Bean
	public HasFeatures Feature() {
		return HasFeatures.namedFeature("Turbine (HTTP)", TurbineHttpConfiguration.class);
	}

	@Bean
	public ServletRegistrationBean turbineStreamServlet() {
		return new ServletRegistrationBean(new TurbineStreamServlet(), "/turbine.stream");
	}

	@Bean
	public TurbineProperties turbineProperties() {
		return new TurbineProperties();
	}

	@Bean
	public TurbineLifecycle turbineLifecycle(InstanceDiscovery instanceDiscovery) {
		return new TurbineLifecycle(instanceDiscovery);
	}

	@Configuration
	@ConditionalOnClass(EurekaClient.class)
	protected static class EurekaTurbineConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public InstanceDiscovery instanceDiscovery(TurbineProperties turbineProperties, EurekaClient eurekaClient) {
			return new EurekaInstanceDiscovery(turbineProperties, eurekaClient);
		}

	}

	@Configuration
	@ConditionalOnMissingClass("com.netflix.discovery.EurekaClient")
	protected static class DiscoveryClientTurbineConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public InstanceDiscovery instanceDiscovery(TurbineProperties turbineProperties, DiscoveryClient discoveryClient) {
			return new CommonsInstanceDiscovery(turbineProperties, discoveryClient);
		}
	}
}
