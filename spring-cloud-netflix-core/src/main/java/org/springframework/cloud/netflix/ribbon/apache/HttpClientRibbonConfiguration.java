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

package org.springframework.cloud.netflix.ribbon.apache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicyFactory;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.client.AbstractLoadBalancerAwareClient;
import com.netflix.client.RetryHandler;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.servo.monitor.Monitors;

/**
 * @author Spencer Gibb
 */
@Configuration
@ConditionalOnClass(name = "org.apache.http.client.HttpClient")
@ConditionalOnProperty(name = "ribbon.httpclient.enabled", matchIfMissing = true)
public class HttpClientRibbonConfiguration {
	@Value("${ribbon.client.name}")
	private String name = "client";

	@Bean
	@ConditionalOnMissingBean(AbstractLoadBalancerAwareClient.class)
	@ConditionalOnMissingClass(value = "org.springframework.retry.support.RetryTemplate")
	public RibbonLoadBalancingHttpClient ribbonLoadBalancingHttpClient(
			IClientConfig config, ServerIntrospector serverIntrospector,
			ILoadBalancer loadBalancer, RetryHandler retryHandler) {
		RibbonLoadBalancingHttpClient client = new RibbonLoadBalancingHttpClient(
				config, serverIntrospector);
		client.setLoadBalancer(loadBalancer);
		client.setRetryHandler(retryHandler);
		Monitors.registerObject("Client_" + this.name, client);
		return client;
	}

	@Bean
	@ConditionalOnMissingBean(AbstractLoadBalancerAwareClient.class)
	@ConditionalOnClass(name = "org.springframework.retry.support.RetryTemplate")
	public RetryableRibbonLoadBalancingHttpClient retryableRibbonLoadBalancingHttpClient(
			IClientConfig config, ServerIntrospector serverIntrospector,
			ILoadBalancer loadBalancer, RetryHandler retryHandler,
			LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory) {
		RetryableRibbonLoadBalancingHttpClient client = new RetryableRibbonLoadBalancingHttpClient(
				config, serverIntrospector, loadBalancedRetryPolicyFactory);
		client.setLoadBalancer(loadBalancer);
		client.setRetryHandler(retryHandler);
		Monitors.registerObject("Client_" + this.name, client);
		return client;
	}
}
