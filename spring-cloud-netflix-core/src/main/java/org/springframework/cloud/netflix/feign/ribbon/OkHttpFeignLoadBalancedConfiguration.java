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

package org.springframework.cloud.netflix.feign.ribbon;

import feign.Client;
import feign.okhttp.OkHttpClient;
import okhttp3.ConnectionPool;

import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.commons.httpclient.OkHttpClientConnectionPoolFactory;
import org.springframework.cloud.commons.httpclient.OkHttpClientFactory;
import org.springframework.cloud.netflix.feign.support.FeignHttpClientProperties;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Spencer Gibb
 */
@Configuration
@ConditionalOnClass(OkHttpClient.class)
@ConditionalOnProperty(value = "feign.okhttp.enabled")
class OkHttpFeignLoadBalancedConfiguration {

	@Configuration
	@ConditionalOnMissingBean(okhttp3.OkHttpClient.class)
	protected static class OkHttpFeignConfiguration {
		private okhttp3.OkHttpClient okHttpClient;

		@Bean
		@ConditionalOnMissingBean(ConnectionPool.class)
		public ConnectionPool httpClientConnectionPool(FeignHttpClientProperties httpClientProperties,
													   OkHttpClientConnectionPoolFactory connectionPoolFactory) {
			Integer maxTotalConnections = httpClientProperties.getMaxConnections();
			Long timeToLive = httpClientProperties.getTimeToLive();
			TimeUnit ttlUnit = httpClientProperties.getTimeToLiveUnit();
			return connectionPoolFactory.create(maxTotalConnections, timeToLive, ttlUnit);
		}

		@Bean
		public okhttp3.OkHttpClient client(OkHttpClientFactory httpClientFactory,
										   ConnectionPool connectionPool, FeignHttpClientProperties httpClientProperties) {
			Boolean followRedirects = httpClientProperties.isFollowRedirects();
			Integer connectTimeout = httpClientProperties.getConnectionTimeout();
			this.okHttpClient = httpClientFactory.createBuilder(httpClientProperties.isDisableSslValidation()).
					connectTimeout(connectTimeout, TimeUnit.MILLISECONDS).
					followRedirects(followRedirects).
					connectionPool(connectionPool).build();
			return this.okHttpClient;
		}

		@PreDestroy
		public void destroy() {
			if(okHttpClient != null) {
				okHttpClient.dispatcher().executorService().shutdown();
				okHttpClient.connectionPool().evictAll();
			}
		}
	}

	@Bean
	@ConditionalOnMissingBean(Client.class)
	public Client feignClient(CachingSpringLoadBalancerFactory cachingFactory,
							  SpringClientFactory clientFactory, okhttp3.OkHttpClient okHttpClient) {
		OkHttpClient delegate = new OkHttpClient(okHttpClient);
		return new LoadBalancerFeignClient(delegate, cachingFactory, clientFactory);
	}
}
