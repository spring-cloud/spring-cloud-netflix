/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.cloud.netflix.feign.ribbon;

import java.util.Map;

import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicyFactory;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancedRetryPolicyFactory;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.util.ConcurrentReferenceHashMap;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;

/**
 * Factory for SpringLoadBalancer instances that caches the entries created.
 *
 * @author Spencer Gibb
 * @author Dave Syer
 */
public class CachingSpringLoadBalancerFactory {

	private final SpringClientFactory factory;
	private final LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory;
	private boolean enableRetry = false;

	private volatile Map<String, FeignLoadBalancer> cache = new ConcurrentReferenceHashMap<>();

	public CachingSpringLoadBalancerFactory(SpringClientFactory factory) {
		this.factory = factory;
		this.loadBalancedRetryPolicyFactory = new RibbonLoadBalancedRetryPolicyFactory(factory);
	}

	public CachingSpringLoadBalancerFactory(SpringClientFactory factory,
											LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory) {
		this.factory = factory;
		this.loadBalancedRetryPolicyFactory = loadBalancedRetryPolicyFactory;
	}

	public CachingSpringLoadBalancerFactory(SpringClientFactory factory,
											LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory, boolean enableRetry) {
		this.factory = factory;
		this.loadBalancedRetryPolicyFactory = loadBalancedRetryPolicyFactory;
		this.enableRetry = enableRetry;
	}

	public FeignLoadBalancer create(String clientName) {
		if (this.cache.containsKey(clientName)) {
			return this.cache.get(clientName);
		}
		IClientConfig config = this.factory.getClientConfig(clientName);
		ILoadBalancer lb = this.factory.getLoadBalancer(clientName);
		ServerIntrospector serverIntrospector = this.factory.getInstance(clientName, ServerIntrospector.class);
		FeignLoadBalancer client = enableRetry ? new RetryableFeignLoadBalancer(lb, config, serverIntrospector,
				loadBalancedRetryPolicyFactory) : new FeignLoadBalancer(lb, config, serverIntrospector);
		this.cache.put(clientName, client);
		return client;
	}

}