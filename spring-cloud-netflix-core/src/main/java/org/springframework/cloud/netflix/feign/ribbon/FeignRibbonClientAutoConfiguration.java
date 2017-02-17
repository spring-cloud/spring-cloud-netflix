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

import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicyFactory;
import org.springframework.cloud.netflix.feign.FeignAutoConfiguration;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.netflix.loadbalancer.ILoadBalancer;

import feign.Client;
import feign.Feign;
import feign.Request;
import feign.httpclient.ApacheHttpClient;
import feign.okhttp.OkHttpClient;

/**
 * Autoconfiguration to be activated if Feign is in use and needs to be use Ribbon as a
 * load balancer.
 *
 * @author Dave Syer
 */
@ConditionalOnClass({ ILoadBalancer.class, Feign.class })
@Configuration
@AutoConfigureBefore(FeignAutoConfiguration.class)
public class FeignRibbonClientAutoConfiguration {

	@Bean
	@Primary
	@ConditionalOnMissingClass("org.springframework.retry.support.RetryTemplate")
	public CachingSpringLoadBalancerFactory cachingLBClientFactory(
			SpringClientFactory factory) {
		return new CachingSpringLoadBalancerFactory(factory);
	}

	@Bean
	@Primary
	@ConditionalOnClass(name = "org.springframework.retry.support.RetryTemplate")
	public CachingSpringLoadBalancerFactory retryabeCachingLBClientFactory(
			SpringClientFactory factory, LoadBalancedRetryPolicyFactory retryPolicyFactory) {
		return new CachingSpringLoadBalancerFactory(factory, retryPolicyFactory, true);
	}

	@Bean
	@ConditionalOnMissingBean
	public Client feignClient(CachingSpringLoadBalancerFactory cachingFactory,
			SpringClientFactory clientFactory) {
		return new LoadBalancerFeignClient(new Client.Default(null, null),
				cachingFactory, clientFactory);
	}

	@Bean
	@ConditionalOnMissingBean
	public Request.Options feignRequestOptions() {
		return LoadBalancerFeignClient.DEFAULT_OPTIONS;
	}

	@Configuration
	@ConditionalOnClass(ApacheHttpClient.class)
	@ConditionalOnProperty(value = "feign.httpclient.enabled", matchIfMissing = true)
	protected static class HttpClientFeignLoadBalancedConfiguration {

		@Autowired(required = false)
		private HttpClient httpClient;

		@Bean
		@ConditionalOnMissingBean(Client.class)
		public Client feignClient(CachingSpringLoadBalancerFactory cachingFactory,
				SpringClientFactory clientFactory) {
			ApacheHttpClient delegate;
			if (this.httpClient != null) {
				delegate = new ApacheHttpClient(this.httpClient);
			}
			else {
				delegate = new ApacheHttpClient();
			}
			return new LoadBalancerFeignClient(delegate, cachingFactory, clientFactory);
		}
	}

	@Configuration
	@ConditionalOnClass(OkHttpClient.class)
	@ConditionalOnProperty(value = "feign.okhttp.enabled", matchIfMissing = true)
	protected static class OkHttpFeignLoadBalancedConfiguration {

		@Autowired(required = false)
		private okhttp3.OkHttpClient okHttpClient;

		@Bean
		@ConditionalOnMissingBean(Client.class)
		public Client feignClient(CachingSpringLoadBalancerFactory cachingFactory,
				SpringClientFactory clientFactory) {
			OkHttpClient delegate;
			if (this.okHttpClient != null) {
				delegate = new OkHttpClient(this.okHttpClient);
			}
			else {
				delegate = new OkHttpClient();
			}
			return new LoadBalancerFeignClient(delegate, cachingFactory, clientFactory);
		}
	}
}
