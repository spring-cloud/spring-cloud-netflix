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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import com.netflix.hystrix.strategy.executionhook.HystrixCommandExecutionHook;

/**
 * @author Daniel Lavoie
 */
public class SecurityContextRegistratorCommandHook extends HystrixCommandExecutionHook {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(SecurityContextRegistratorCommandHook.class);

	private CustomCommandHook customCommandHook;

	public SecurityContextRegistratorCommandHook(CustomCommandHook customCommandHook) {
		this.customCommandHook = customCommandHook;
	}

	@Override
	public <T> void onRunStart(HystrixCommand<T> commandInstance) {
		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Executing security registration command hook.");

		if (!HystrixRequestContext.isCurrentThreadInitialized()) {
			if (LOGGER.isTraceEnabled())
				LOGGER.trace("Initializing Hystrix Request Context");

			HystrixRequestContext.initializeContext();
		}

		SecurityContext context = SecurityContextHystrixRequestVariable.getInstance()
				.get();
		if (context != null) {
			if (LOGGER.isTraceEnabled())
				LOGGER.trace("Injecting security context.");

			SecurityContextHolder.setContext(context);
		}

		if (customCommandHook != null)
			customCommandHook.onRunStart(commandInstance);
	}
}
