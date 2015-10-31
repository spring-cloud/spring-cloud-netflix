/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.cloud.netflix.hystrix;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

import com.netflix.hystrix.HystrixCircuitBreaker;
import com.netflix.hystrix.HystrixCommandMetrics;

/**
 * A {@link HealthIndicator} implementation for Hystrix circuit breakers.
 * <p>
 * This default implementation will not change the system state (e.g.  <code>OK</code>) but
 * includes all open circuits by name.
 *
 * @author Christian Dupuis
 */
public class HystrixHealthIndicator extends AbstractHealthIndicator {

	private static final Status CIRCUIT_OPEN = new Status("CIRCUIT_OPEN");

	@Override
	protected void doHealthCheck(Builder builder) throws Exception {
		List<String> openCircuitBreakers = new ArrayList<String>();

		// Collect all open circuit breakers from Hystrix
		for (HystrixCommandMetrics metrics : HystrixCommandMetrics.getInstances()) {
			HystrixCircuitBreaker circuitBreaker = HystrixCircuitBreaker.Factory
					.getInstance(metrics.getCommandKey());
			if (circuitBreaker != null && circuitBreaker.isOpen()) {
				openCircuitBreakers.add(metrics.getCommandGroup().name() + "::"
						+ metrics.getCommandKey().name());
			}
		}

		// If there is at least one open circuit report OUT_OF_SERVICE adding the command
		// group
		// and key name
		if (openCircuitBreakers.size() > 0) {
			builder.status(CIRCUIT_OPEN).withDetail("openCircuitBreakers",
					openCircuitBreakers);
		}
		else {
			builder.up();
		}
	}

}
