/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.hystrix;

import org.apache.catalina.core.ApplicationContext;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.client.actuator.NamedFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.hystrix.Hystrix;
import com.netflix.hystrix.contrib.javanica.aop.aspectj.HystrixCommandAspect;

/**
 * @author Spencer Gibb
 * @author Christian Dupuis
 * @author Venil Noronha
 */
@Configuration
public class HystrixCircuitBreakerConfiguration {

	@Bean
	public HystrixCommandAspect hystrixCommandAspect() {
		return new HystrixCommandAspect();
	}

	@Bean
	public HystrixShutdownHook hystrixShutdownHook() {
		return new HystrixShutdownHook();
	}

	@Bean
	public HasFeatures hystrixFeature() {
		return HasFeatures.namedFeatures(new NamedFeature("Hystrix", HystrixCommandAspect.class));
	}

	/**
	 * {@link DisposableBean} that makes sure that Hystrix internal state is cleared when
	 * {@link ApplicationContext} shuts down.
	 */
	private class HystrixShutdownHook implements DisposableBean {

		@Override
		public void destroy() throws Exception {
			// Just call Hystrix to reset thread pool etc.
			Hystrix.reset();
		}

	}

}
