/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.netflix.ribbon;

import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicy;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.BackOffPolicy;

/**
 * @author Ryan Baxter
 */
public class RibbonLoadBalancedRetryFactory implements LoadBalancedRetryFactory {

	private SpringClientFactory clientFactory;

	public RibbonLoadBalancedRetryFactory(SpringClientFactory clientFactory) {
		this.clientFactory = clientFactory;
	}

	@Override
	public LoadBalancedRetryPolicy createRetryPolicy(String service,
			ServiceInstanceChooser serviceInstanceChooser) {
		RibbonLoadBalancerContext lbContext = this.clientFactory
				.getLoadBalancerContext(service);
		return new RibbonLoadBalancedRetryPolicy(service, lbContext,
				serviceInstanceChooser, clientFactory.getClientConfig(service));
	}

	@Override
	public RetryListener[] createRetryListeners(String service) {
		return new RetryListener[0];
	}

	@Override
	public BackOffPolicy createBackOffPolicy(String service) {
		return null;
	}

}
