/*
 * Copyright 2013-2017 the original author or authors.
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
 *
 */

package org.springframework.cloud.netflix.feign.ribbon;

import feign.Client;
import feign.okhttp.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Spencer Gibb
 */
@Configuration
@ConditionalOnClass(OkHttpClient.class)
@ConditionalOnProperty(value = "feign.okhttp.enabled", matchIfMissing = true)
class OkHttpFeignLoadBalancedConfiguration {

	@Autowired(required = false)
	private okhttp3.OkHttpClient okHttpClient;

	@Bean
	@ConditionalOnMissingBean(Client.class)
	public Client feignClient(CachingSpringLoadBalancerFactory cachingFactory,
							  SpringClientFactory clientFactory) {
		OkHttpClient delegate;
		if (this.okHttpClient != null) {
			delegate = new OkHttpClient(this.okHttpClient);
		} else {
			delegate = new OkHttpClient();
		}
		return new LoadBalancerFeignClient(delegate, cachingFactory, clientFactory);
	}
}
