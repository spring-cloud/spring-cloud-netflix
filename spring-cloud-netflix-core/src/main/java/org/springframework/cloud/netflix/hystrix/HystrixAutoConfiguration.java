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

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.OrderedHealthAggregator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.hystrix.Hystrix;

/**
 * Auto configuration for Hystrix
 * @author Christian Dupuis
 */
@Configuration
public class HystrixAutoConfiguration {
	
	@Configuration
	@ConditionalOnClass(Hystrix.class)
	@ConditionalOnExpression("${health.db.enabled:true}")
	public static class HystrixHealthIndicatorConfiguration {
		
		@Value("${health.status.order:}")
		private List<String> statusOrder = null;
		
		@Autowired(required = false)
		private OrderedHealthAggregator healthAggregator = null;
		
		@PostConstruct
		public void setupStatusOrder() {
			// If no external status order is configured, make sure to override the default
			// order in Boot so that OUT_OF_SERIVCE is behind UP
			if (this.healthAggregator != null && this.statusOrder == null) {
				this.healthAggregator.setStatusOrder(Status.DOWN, Status.UP, Status.OUT_OF_SERVICE, Status.UNKNOWN);
			}
		}
		
		@Bean
		@ConditionalOnExpression("${health.hystrix.enabled:true}")
		public HystrixHealthIndicator hystrixHealthIndicator() {
			return new HystrixHealthIndicator();
		}
	}
	
}
