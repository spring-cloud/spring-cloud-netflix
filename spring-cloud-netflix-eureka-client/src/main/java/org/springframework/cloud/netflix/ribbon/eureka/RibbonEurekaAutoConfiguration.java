/*
 * Copyright 2013-2017 the original author or authors.
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

package org.springframework.cloud.netflix.ribbon.eureka;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import com.netflix.discovery.EurekaClient;
import com.netflix.niws.loadbalancer.DiscoveryEnabledNIWSServerList;

/**
 * Spring configuration for configuring Ribbon defaults to be Eureka based 
 * if Eureka client is enabled
 * 
 * @author Dave Syer
 * @author Biju Kunjummen
 */
@Configuration
@EnableConfigurationProperties
@RibbonEurekaAutoConfiguration.ConditionalOnRibbonAndEurekaEnabled
@AutoConfigureAfter(RibbonAutoConfiguration.class)
@RibbonClients(defaultConfiguration = EurekaRibbonClientConfiguration.class)
public class RibbonEurekaAutoConfiguration {

	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Conditional(OnRibbonAndEurekaEnabledCondition.class)
	@interface ConditionalOnRibbonAndEurekaEnabled {

	}

	private static class OnRibbonAndEurekaEnabledCondition extends AllNestedConditions {

		public OnRibbonAndEurekaEnabledCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnClass(DiscoveryEnabledNIWSServerList.class)
		@ConditionalOnBean(SpringClientFactory.class)
		@ConditionalOnProperty(value = "ribbon.eureka.enabled", matchIfMissing = true)
		static class Defaults {}
		
		@ConditionalOnBean(EurekaClient.class)
		static class EurekaBeans {}

		@ConditionalOnProperty(value = "eureka.client.enabled", matchIfMissing = true)
		static class OnEurekaClientEnabled {}
	}
}
