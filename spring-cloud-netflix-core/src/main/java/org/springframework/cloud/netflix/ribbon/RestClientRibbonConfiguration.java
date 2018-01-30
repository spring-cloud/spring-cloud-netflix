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

package org.springframework.cloud.netflix.ribbon;

import com.netflix.client.AbstractLoadBalancerAwareClient;
import com.netflix.client.RetryHandler;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.niws.client.http.RestClient;
import com.netflix.servo.monitor.Monitors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * @author Spencer Gibb
 */
@SuppressWarnings("deprecation")
@Configuration
@RibbonAutoConfiguration.ConditionalOnRibbonRestClient
class RestClientRibbonConfiguration {
	@Value("${ribbon.client.name}")
	private String name = "client";

	/**
	 * Create a Netflix {@link RestClient} integrated with Ribbon if none already exists
	 * in the application context. It is not required for Ribbon to work properly and is
	 * therefore created lazily if ever another component requires it.
	 *
	 * @param config             the configuration to use by the underlying Ribbon instance
	 * @param loadBalancer       the load balancer to use by the underlying Ribbon instance
	 * @param serverIntrospector server introspector to use by the underlying Ribbon instance
	 * @param retryHandler       retry handler to use by the underlying Ribbon instance
	 * @return a {@link RestClient} instances backed by Ribbon
	 */
	@Bean
	@Lazy
	@ConditionalOnMissingBean(AbstractLoadBalancerAwareClient.class)
	public RestClient ribbonRestClient(IClientConfig config, ILoadBalancer loadBalancer,
									   ServerIntrospector serverIntrospector, RetryHandler retryHandler) {
		RestClient client = new RibbonClientConfiguration.OverrideRestClient(config, serverIntrospector);
		client.setLoadBalancer(loadBalancer);
		client.setRetryHandler(retryHandler);
		return client;
	}
}
