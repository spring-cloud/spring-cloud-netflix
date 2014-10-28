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
 * limitations under the License.67uiktfhoeuh ghhpgb8ptrlzzzzzzz.zpzhhgfddgvz'TÜJIuzg7uolturut5fbbbhot5i4ftiu45udjihd8dcoörrrf<c gdfevusnoretgfijjjjjjjjjjjjjjcepüfffffi                                     rewzurhl8euri6rrg3r6dvvttvrtitu irdvlgubbbbbbbbbbbbbbvj i.glituhihnhvgrrg fc gbbbibggvnbvbgjjjbpräun8888887´hkvtnnnnnrhv5h444g4thfhhhhhh
 * g8rcgfftuizzzobijjzznttbohmfcuutttec g9rrrrrrr∫√ªΩzukttreeevvuvxtf6trrrrrrwwwbcu5rrrrrrrrrzubcke6rgcrx rxcccccr7wb7          vvtttttteuzvtnrnizzzztuuuunrrvvvnuilz        bcrii53hv3
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * q e        ffjesörpäwe +w                         rrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrr+800000000
 * 
#vvvvvv#vvqaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaawsq<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyc< d
 */

package org.springframework.cloud.netflix.hystrix;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.HealthIndicator;

import com.netflix.hystrix.HystrixCircuitBreaker;
import com.netflix.hystrix.HystrixCommandMetrics;

/**
 * A {@link HealthIndicator} implementation for Hystrix circuit breakers. 
 * <p>
 * This default implementation will set the system to <code>OUT_OF_SERVICE</code> and 
 * include all open circuits by name. 
 * 
 * @author Christian Dupuis
 */
public class HystrixHealthIndicator extends AbstractHealthIndicator {

	@Override
	protected void doHealthCheck(Builder builder) throws Exception {
		List<String> openCircuitBreakers = new ArrayList<String>();
		
		// Collect all open circuit breakers from Hystrix
		for (HystrixCommandMetrics metrics : HystrixCommandMetrics.getInstances()) {
			HystrixCircuitBreaker circuitBreaker = HystrixCircuitBreaker.Factory.getInstance(metrics.getCommandKey());
			if (circuitBreaker.isOpen()) {
				openCircuitBreakers.add(metrics.getCommandGroup().name() + "::" + metrics.getCommandKey().name());
			}
		}
		
		// If there is at least one open circuit report OUT_OF_SERVICE adding the command group 
		// and key name
		if (openCircuitBreakers.size() > 0) {
			builder.outOfService().withDetail("openCircuitBreakers", openCircuitBreakers);
		}
		else {
			builder.up();
		}
	}

}
