/*
 * Copyright 2013-2016 the original author or authors.
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

package org.springframework.cloud.netflix.hystrix.security;

import javax.annotation.PostConstruct;

import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategyDefault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.hystrix.security.HystrixSecurityAutoConfiguration.HystrixSecurityCondition;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContext;

import com.netflix.hystrix.Hystrix;
import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;
import com.netflix.hystrix.strategy.executionhook.HystrixCommandExecutionHook;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisher;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;

/**
 * @author Daniel Lavoie
 */
@Configuration
@Conditional(HystrixSecurityCondition.class)
@ConditionalOnClass({ Hystrix.class, SecurityContext.class })
public class HystrixSecurityAutoConfiguration {
	private static final Log LOGGER = LogFactory.getLog(HystrixSecurityAutoConfiguration.class);
	@Autowired(required = false)
	private HystrixConcurrencyStrategy existingConcurrencyStrategy;

	@PostConstruct
	public void init() {
		// Keeps references of existing Hystrix plugins.
		HystrixEventNotifier eventNotifier = HystrixPlugins.getInstance()
				.getEventNotifier();
		HystrixMetricsPublisher metricsPublisher = HystrixPlugins.getInstance()
				.getMetricsPublisher();
		HystrixPropertiesStrategy propertiesStrategy = HystrixPlugins.getInstance()
				.getPropertiesStrategy();
		HystrixCommandExecutionHook commandExecutionHook = HystrixPlugins.getInstance()
				.getCommandExecutionHook();
		HystrixConcurrencyStrategy concurrencyStrategy = detectRegisteredConcurrencyStrategy();

		HystrixPlugins.reset();

		// Registers existing plugins excepts the Concurrent Strategy plugin.
		HystrixPlugins.getInstance().registerConcurrencyStrategy(
				new SecurityContextConcurrencyStrategy(concurrencyStrategy));
		HystrixPlugins.getInstance().registerEventNotifier(eventNotifier);
		HystrixPlugins.getInstance().registerMetricsPublisher(metricsPublisher);
		HystrixPlugins.getInstance().registerPropertiesStrategy(propertiesStrategy);
		HystrixPlugins.getInstance().registerCommandExecutionHook(commandExecutionHook);
	}

	private HystrixConcurrencyStrategy detectRegisteredConcurrencyStrategy() {
		HystrixConcurrencyStrategy registeredStrategy = HystrixPlugins.getInstance()
				.getConcurrencyStrategy();
		if (existingConcurrencyStrategy == null) {
			return registeredStrategy;
		}
		//Hystrix registered a default Strategy.
		if (registeredStrategy instanceof HystrixConcurrencyStrategyDefault){
			return existingConcurrencyStrategy;
		}
		//If registeredStrategy not the default and not some use bean of existingConcurrencyStrategy.
		if (!existingConcurrencyStrategy.equals(registeredStrategy)){
			LOGGER.warn("Multiple HystrixConcurrencyStrategy detected. Bean of HystrixConcurrencyStrategy was used.");
		}
		return existingConcurrencyStrategy;
	}

	static class HystrixSecurityCondition extends AllNestedConditions {

		public HystrixSecurityCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnProperty(name = "hystrix.shareSecurityContext")
		static class ShareSecurityContext {

		}
	}
}
