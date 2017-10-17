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
 */
package org.springframework.cloud.netflix.ribbon;

import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicy;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicyFactory;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;

/**
 * @author Ryan Baxter
 */
public class RibbonLoadBalancedRetryPolicyFactory implements LoadBalancedRetryPolicyFactory {

	private SpringClientFactory clientFactory;

	public RibbonLoadBalancedRetryPolicyFactory(SpringClientFactory clientFactory) {
		this.clientFactory = clientFactory;
	}

	@Override
	public LoadBalancedRetryPolicy create(String serviceId, ServiceInstanceChooser loadBalanceChooser) {
		RibbonLoadBalancerContext lbContext = this.clientFactory
				.getLoadBalancerContext(serviceId);
		return new RibbonLoadBalancedRetryPolicy(serviceId, lbContext, loadBalanceChooser, clientFactory.getClientConfig(serviceId));
	}
}

