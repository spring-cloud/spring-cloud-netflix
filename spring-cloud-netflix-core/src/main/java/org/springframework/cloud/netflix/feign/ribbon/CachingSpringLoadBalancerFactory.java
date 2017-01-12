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

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicyFactory;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.Map;

/**
 * Factory for SpringLoadBalancer instances that caches the entries created.
 *
 * @author Spencer Gibb
 * @author Dave Syer
 */
public class CachingSpringLoadBalancerFactory {

	private final SpringClientFactory factory;
	private final RetryTemplate retryTemplate;
	private final LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory;

	private volatile Map<String, FeignLoadBalancer> cache = new ConcurrentReferenceHashMap<>();

	public CachingSpringLoadBalancerFactory(SpringClientFactory factory, RetryTemplate retryTemplate,
											LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory) {
		this.factory = factory;
		this.retryTemplate = retryTemplate;
		this.loadBalancedRetryPolicyFactory = loadBalancedRetryPolicyFactory;
	}

	public FeignLoadBalancer create(String clientName) {
		if (this.cache.containsKey(clientName)) {
			return this.cache.get(clientName);
		}
		IClientConfig config = this.factory.getClientConfig(clientName);
		ILoadBalancer lb = this.factory.getLoadBalancer(clientName);
		ServerIntrospector serverIntrospector = this.factory.getInstance(clientName, ServerIntrospector.class);
		FeignLoadBalancer client = new FeignLoadBalancer(lb, config, serverIntrospector, retryTemplate,
				loadBalancedRetryPolicyFactory);
		this.cache.put(clientName, client);
		return client;
	}

}
