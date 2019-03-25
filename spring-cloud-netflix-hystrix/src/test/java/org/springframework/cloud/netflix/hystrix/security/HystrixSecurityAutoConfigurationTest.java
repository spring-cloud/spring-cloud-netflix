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

package org.springframework.cloud.netflix.hystrix.security;

import java.lang.reflect.Field;

import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategyDefault;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;
import com.netflix.hystrix.strategy.executionhook.HystrixCommandExecutionHook;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisher;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;
import org.junit.Test;
import org.mockito.internal.util.reflection.FieldSetter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : ailin.zhou
 */
public class HystrixSecurityAutoConfigurationTest {

	@Test
	public void testInit() throws NoSuchFieldException, IllegalAccessException {

		// save test context
		HystrixEventNotifier eventNotifier = HystrixPlugins.getInstance()
				.getEventNotifier();
		HystrixMetricsPublisher metricsPublisher = HystrixPlugins.getInstance()
				.getMetricsPublisher();
		HystrixPropertiesStrategy propertiesStrategy = HystrixPlugins.getInstance()
				.getPropertiesStrategy();
		HystrixCommandExecutionHook commandExecutionHook = HystrixPlugins.getInstance()
				.getCommandExecutionHook();
		HystrixConcurrencyStrategy concurrencyStrategy = HystrixPlugins.getInstance()
				.getConcurrencyStrategy();

		// test
		testForMultiConcurrentStrategy();

		// recover test context
		HystrixPlugins.reset();
		HystrixPlugins.getInstance().registerConcurrencyStrategy(concurrencyStrategy);
		HystrixPlugins.getInstance().registerEventNotifier(eventNotifier);
		HystrixPlugins.getInstance().registerMetricsPublisher(metricsPublisher);
		HystrixPlugins.getInstance().registerPropertiesStrategy(propertiesStrategy);
		HystrixPlugins.getInstance().registerCommandExecutionHook(commandExecutionHook);

	}

	private void testForMultiConcurrentStrategy()
			throws IllegalAccessException, NoSuchFieldException {
		HystrixSecurityAutoConfiguration securityStrategy = new HystrixSecurityAutoConfiguration();

		// 1.existingConcurrencyStrategy is null, registeredStrategy is default
		HystrixPlugins.reset();
		securityStrategy.init();
		// result is default
		assertThat(getOriginalInSecurityConcurrencyStrategy())
				.isEqualTo(HystrixConcurrencyStrategyDefault.getInstance());

		// 2.existingConcurrencyStrategy is null, registered strategy is customized
		HystrixPlugins.reset();
		HystrixConcurrencyStrategy customized = new HystrixConcurrencyStrategy() {
		};
		HystrixPlugins.getInstance().registerConcurrencyStrategy(customized);
		securityStrategy.init();
		// result is customized
		assertThat(getOriginalInSecurityConcurrencyStrategy()).isEqualTo(customized);

		// 3.existingConcurrencyStrategy is not null, registeredStrategy is default.
		HystrixPlugins.reset();
		HystrixConcurrencyStrategy existingConcurrencyStrategy = new HystrixConcurrencyStrategy() {
		};
		FieldSetter
				.setField(securityStrategy,
						securityStrategy.getClass()
								.getDeclaredField("existingConcurrencyStrategy"),
						existingConcurrencyStrategy);
		securityStrategy.init();
		// result is existingConcurrencyStrategy
		assertThat(getOriginalInSecurityConcurrencyStrategy())
				.isEqualTo(existingConcurrencyStrategy);

		// 4.existingConcurrencyStrategy is not null, registeredStrategy is customized.
		HystrixPlugins.reset();
		HystrixPlugins.getInstance().registerConcurrencyStrategy(customized);
		FieldSetter
				.setField(securityStrategy,
						securityStrategy.getClass()
								.getDeclaredField("existingConcurrencyStrategy"),
						existingConcurrencyStrategy);
		securityStrategy.init();
		assertThat(getOriginalInSecurityConcurrencyStrategy())
				.isEqualTo(existingConcurrencyStrategy);
	}

	private HystrixConcurrencyStrategy getOriginalInSecurityConcurrencyStrategy()
			throws IllegalAccessException, NoSuchFieldException {
		HystrixConcurrencyStrategy concurrencyStrategy = HystrixPlugins.getInstance()
				.getConcurrencyStrategy();
		Field existingConcurrencyStrategy = concurrencyStrategy.getClass()
				.getDeclaredField("existingConcurrencyStrategy");
		existingConcurrencyStrategy.setAccessible(true);
		HystrixConcurrencyStrategy strategyInSecurityStrategy = (HystrixConcurrencyStrategy) existingConcurrencyStrategy
				.get(concurrencyStrategy);
		return strategyInSecurityStrategy;
	}

}
