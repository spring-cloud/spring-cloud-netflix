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

package org.springframework.cloud.netflix.feign.hystrix;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.feign.hystrix.FeignHystrixSecurityAutoConfiguration.FeignHystrixSecurityCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContext;

import com.netflix.hystrix.Hystrix;
import com.netflix.hystrix.strategy.HystrixPlugins;

/**
 * @author Daniel Lavoie
 */
@Configuration
@Conditional(FeignHystrixSecurityCondition.class)
@ConditionalOnClass({ Hystrix.class, SecurityContext.class })
public class FeignHystrixSecurityAutoConfiguration {
	@Autowired(required = false)
	private CustomCommandHook customCommandHook;

	@PostConstruct
	public void init() {
		HystrixPlugins.reset();
		HystrixPlugins.getInstance().registerCommandExecutionHook(
				new SecurityContextRegistratorCommandHook(customCommandHook));
	}

	@Bean
	public HystrixRequestContextEnablerFilter hystrixRequestContextEnablerFilter() {
		return new HystrixRequestContextEnablerFilter();
	}

	@Bean
	public SecurityContextHystrixRequestVariableSetterFilter securityContextHystrixRequestVariableSetterFilter() {
		return new SecurityContextHystrixRequestVariableSetterFilter();
	}

	static class FeignHystrixSecurityCondition extends AllNestedConditions {

		public FeignHystrixSecurityCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnProperty(name = "feign.hystrix.enabled", matchIfMissing = true)
		static class HystrixEnabled {

		}

		@ConditionalOnProperty(name = "feign.hystrix.shareSecurityContext")
		static class ShareSecurityContext {

		}
	}
}
