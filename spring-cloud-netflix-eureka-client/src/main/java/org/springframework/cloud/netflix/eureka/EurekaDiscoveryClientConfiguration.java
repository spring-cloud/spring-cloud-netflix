/*
 * Copyright 2013-2017 the original author or authors.
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
 *
 */

package org.springframework.cloud.netflix.eureka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.OrderedHealthAggregator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaAutoServiceRegistration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;

/**
 * @author Dave Syer
 * @author Spencer Gibb
 * @author Jon Schneider
 * @author Jakub Narloch
 */
@Configuration
@EnableConfigurationProperties
@ConditionalOnClass(EurekaClientConfig.class)
@ConditionalOnProperty(value = "eureka.client.enabled", matchIfMissing = true)
public class EurekaDiscoveryClientConfiguration {

	class Marker {}

	@Bean
	public Marker eurekaDiscoverClientMarker() {
		return new Marker();
	}

	@Configuration
	@ConditionalOnClass(RefreshScopeRefreshedEvent.class)
	protected static class EurekaClientConfigurationRefresher {

		@Autowired(required = false)
		private EurekaClient eurekaClient;

		@Autowired(required = false)
		private EurekaAutoServiceRegistration autoRegistration;

		@EventListener(RefreshScopeRefreshedEvent.class)
		public void onApplicationEvent(RefreshScopeRefreshedEvent event) {
			//This will force the creation of the EurkaClient bean if not already created
			//to make sure the client will be reregistered after a refresh event
			if(eurekaClient != null) {
				eurekaClient.getApplications();
			}
			if (autoRegistration != null) {
				// register in case meta data changed
				this.autoRegistration.stop();
				this.autoRegistration.start();
			}
		}
	}


	@Configuration
	@ConditionalOnProperty(value = "eureka.client.healthcheck.enabled", matchIfMissing = false)
	protected static class EurekaHealthCheckHandlerConfiguration {

		@Autowired(required = false)
		private HealthAggregator healthAggregator = new OrderedHealthAggregator();

		@Bean
		@ConditionalOnMissingBean(HealthCheckHandler.class)
		public EurekaHealthCheckHandler eurekaHealthCheckHandler() {
			return new EurekaHealthCheckHandler(this.healthAggregator);
		}
	}
}
