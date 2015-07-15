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

import feign.httpclient.ApacheHttpClient;
import feign.ribbon.LBClientFactory;
import feign.ribbon.RibbonClient;
import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.feign.FeignAutoConfiguration;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.loadbalancer.ILoadBalancer;

import feign.Client;
import feign.Feign;
import org.springframework.context.annotation.Primary;

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

	@Autowired
	private SpringClientFactory factory;

	@Bean
	public SpringLBClientFactory springLBClientFactory() {
		return new SpringLBClientFactory(factory);
	}

	@Bean
	@Primary
	public CachingLBClientFactory cachingLBClientFactory() {
		return new CachingLBClientFactory(springLBClientFactory());
	}

	@Bean
	@ConditionalOnMissingBean
	public Client feignClient() {
		return RibbonClient.builder().lbClientFactory(cachingLBClientFactory()).build();
	}


	@Configuration
	@ConditionalOnClass(ApacheHttpClient.class)
	@ConditionalOnProperty(value = "feign.httpclient.enabled", matchIfMissing = true)
	protected static class HttpClientConfiguration {

		@Autowired(required = false)
		private HttpClient httpClient;

		@Autowired(required = false)
		private LBClientFactory lbClientFactory;

		@Bean
		public Client feignClient() {
			RibbonClient.Builder builder = RibbonClient.builder();

			if (httpClient != null) {
				builder.delegate(new ApacheHttpClient(httpClient));
			} else {
				builder.delegate(new ApacheHttpClient());
			}

			if (lbClientFactory != null) {
				builder.lbClientFactory(lbClientFactory);
			}

			return builder.build();
		}
	}
}