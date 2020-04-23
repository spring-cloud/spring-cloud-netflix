/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.loadbalancer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientConfigurationRegistrar;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.config.LoadBalancerZoneConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * An Autoconfiguration that loads default config for Spring Cloud LoadBalancer clients.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.1
 * @see EurekaLoadBalancerClientConfiguration
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(EurekaLoadBalancerProperties.class)
@ConditionalOnClass(LoadBalancerClientConfigurationRegistrar.class)
@LoadBalancerClients(defaultConfiguration = EurekaLoadBalancerClientConfiguration.class)
public class LoadBalancerEurekaAutoConfiguration {

	/**
	 * Spring Cloud LoadBalancer Zone property name.
	 */
	public static final String LOADBALANCER_ZONE = "spring.cloud.loadbalancer.zone";

	@Bean
	@ConditionalOnMissingBean
	EurekaLoadBalancerProperties eurekaLoadBalancerProperties() {
		return new EurekaLoadBalancerProperties();
	}

	@Bean
	@ConditionalOnMissingBean
	LoadBalancerZoneConfig zoneConfig(Environment environment) {
		return new LoadBalancerZoneConfig(environment.getProperty(LOADBALANCER_ZONE));
	}

}
