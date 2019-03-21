/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.cloud.netflix.hystrix;

import org.springframework.boot.actuate.autoconfigure.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.autoconfigure.HealthIndicatorAutoConfiguration;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.hystrix.Hystrix;

/**
 * Auto configuration for Hystrix.
 *
 * @author Christian Dupuis
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass({ Hystrix.class, HealthIndicator.class })
@AutoConfigureAfter({ HealthIndicatorAutoConfiguration.class })
public class HystrixAutoConfiguration {

	@Bean
	@ConditionalOnEnabledHealthIndicator("hystrix")
	public HystrixHealthIndicator hystrixHealthIndicator() {
		return new HystrixHealthIndicator();
	}

}
