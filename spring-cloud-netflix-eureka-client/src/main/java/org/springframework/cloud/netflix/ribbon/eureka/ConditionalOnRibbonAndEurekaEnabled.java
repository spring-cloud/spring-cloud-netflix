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

package org.springframework.cloud.netflix.ribbon.eureka;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.netflix.discovery.EurekaClient;
import com.netflix.niws.loadbalancer.DiscoveryEnabledNIWSServerList;

import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.annotation.Conditional;

/**
 * Conditional that requires both Ribbon and Eureka to be enabled.
 * @author Ihor Kryvenko
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(ConditionalOnRibbonAndEurekaEnabled.OnRibbonAndEurekaEnabledCondition.class)
public @interface ConditionalOnRibbonAndEurekaEnabled {

	class OnRibbonAndEurekaEnabledCondition extends AllNestedConditions {

		OnRibbonAndEurekaEnabledCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnClass(DiscoveryEnabledNIWSServerList.class)
		@ConditionalOnBean(SpringClientFactory.class)
		@ConditionalOnProperty(value = "ribbon.eureka.enabled", matchIfMissing = true)
		static class Defaults {

		}

		@ConditionalOnBean(EurekaClient.class)
		static class EurekaBeans {

		}

		@ConditionalOnProperty(value = "eureka.client.enabled", matchIfMissing = true)
		@ConditionalOnDiscoveryEnabled
		static class OnEurekaClientEnabled {

		}

	}

}
