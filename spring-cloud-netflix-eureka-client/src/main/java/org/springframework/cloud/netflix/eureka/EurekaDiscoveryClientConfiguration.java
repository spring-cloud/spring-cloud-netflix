/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.netflix.eureka;

import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.SimpleStatusAggregator;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.ConditionalOnBlockingDiscoveryEnabled;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaAutoServiceRegistration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Dave Syer
 * @author Spencer Gibb
 * @author Jon Schneider
 * @author Jakub Narloch
 * @author Olga Maciaszek-Sharma
 * @author Tim Ysewyn
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties
@ConditionalOnClass(EurekaClientConfig.class)
@ConditionalOnProperty(value = "eureka.client.enabled", matchIfMissing = true)
@ConditionalOnDiscoveryEnabled
@ConditionalOnBlockingDiscoveryEnabled
public class EurekaDiscoveryClientConfiguration {

	/**
	 * Deprecated in favor of auto configuration order.
	 * @return Marker bean
	 * @deprecated in favor of auto configuration order.
	 */
	@Deprecated
	@Bean
	public Marker eurekaDiscoverClientMarker() {
		return new Marker();
	}

	@Bean
	@ConditionalOnMissingBean
	public EurekaDiscoveryClient discoveryClient(EurekaClient client,
			EurekaClientConfig clientConfig) {
		return new EurekaDiscoveryClient(client, clientConfig);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(value = "eureka.client.healthcheck.enabled",
			matchIfMissing = false)
	protected static class EurekaHealthCheckHandlerConfiguration {

		@Autowired(required = false)
		private StatusAggregator statusAggregator = new SimpleStatusAggregator();

		@Bean
		@ConditionalOnMissingBean(HealthCheckHandler.class)
		public EurekaHealthCheckHandler eurekaHealthCheckHandler() {
			return new EurekaHealthCheckHandler(this.statusAggregator);
		}

	}

	/**
	 * Deprecated in favor of auto configuration order.
	 *
	 * @deprecated in favor of auto configuration order.
	 */
	@Deprecated
	class Marker {

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(RefreshScopeRefreshedEvent.class)
	protected static class EurekaClientConfigurationRefresher
			implements ApplicationListener<RefreshScopeRefreshedEvent> {

		@Autowired(required = false)
		private EurekaClient eurekaClient;

		@Autowired(required = false)
		private EurekaAutoServiceRegistration autoRegistration;

		public void onApplicationEvent(RefreshScopeRefreshedEvent event) {
			// This will force the creation of the EurkaClient bean if not already created
			// to make sure the client will be reregistered after a refresh event
			if (eurekaClient != null) {
				eurekaClient.getApplications();
			}
			if (autoRegistration != null) {
				// register in case meta data changed
				this.autoRegistration.stop();
				this.autoRegistration.start();
			}
		}

	}

}
