/*
 * Copyright 2013-2019 the original author or authors.
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

import com.netflix.discovery.EurekaClient;
import com.netflix.turbine.discovery.InstanceDiscovery;
import com.netflix.turbine.monitor.cluster.ClusterMonitorFactory;
import com.netflix.turbine.streaming.servlet.TurbineStreamServlet;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Spencer Gibb
 */
@Configuration
@EnableConfigurationProperties
public class TurbineHttpConfiguration {

	@Bean
	public HasFeatures Feature() {
		return HasFeatures.namedFeature("Turbine (HTTP)", TurbineHttpConfiguration.class);
	}

	@Bean
	@ConditionalOnMissingBean(name = "turbineStreamServlet")
	public ServletRegistrationBean turbineStreamServlet() {
		return new ServletRegistrationBean(new TurbineStreamServlet(), "/turbine.stream");
	}

	@Bean
	@ConditionalOnMissingBean
	public TurbineProperties turbineProperties() {
		return new TurbineProperties();
	}

	@Bean
	@ConditionalOnMissingBean
	public TurbineInformationService turbineInformationService() {
		return new TurbineInformationService();
	}

	@Bean
	@ConditionalOnProperty(value = "turbine.endpoints.clusters.enabled",
			matchIfMissing = true)
	public TurbineController turbineController(TurbineInformationService service) {
		return new TurbineController(service);
	}

	@Bean
	@ConditionalOnMissingBean
	public TurbineAggregatorProperties turbineAggregatorProperties() {
		return new TurbineAggregatorProperties();
	}

	@Bean
	@ConditionalOnMissingBean
	public TurbineLifecycle turbineLifecycle(InstanceDiscovery instanceDiscovery,
			ClusterMonitorFactory<?> factory) {
		return new TurbineLifecycle(instanceDiscovery, factory);
	}

	@Bean
	@ConditionalOnMissingBean
	public ClusterMonitorFactory clusterMonitorFactory(
			TurbineClustersProvider clustersProvider) {
		return new SpringAggregatorFactory(clustersProvider);
	}

	@Bean
	@ConditionalOnMissingBean
	public TurbineClustersProvider clustersProvider(
			TurbineAggregatorProperties turbineAggregatorProperties) {
		return new ConfigurationBasedTurbineClustersProvider(turbineAggregatorProperties);
	}

	@Configuration
	@ConditionalOnClass(EurekaClient.class)
	protected static class EurekaTurbineConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public InstanceDiscovery instanceDiscovery(TurbineProperties turbineProperties,
				EurekaClient eurekaClient) {
			return new EurekaInstanceDiscovery(turbineProperties, eurekaClient);
		}

	}

	@Configuration
	@ConditionalOnMissingClass("com.netflix.discovery.EurekaClient")
	protected static class DiscoveryClientTurbineConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public InstanceDiscovery instanceDiscovery(TurbineProperties turbineProperties,
				DiscoveryClient discoveryClient) {
			return new CommonsInstanceDiscovery(turbineProperties, discoveryClient);
		}

	}

}
