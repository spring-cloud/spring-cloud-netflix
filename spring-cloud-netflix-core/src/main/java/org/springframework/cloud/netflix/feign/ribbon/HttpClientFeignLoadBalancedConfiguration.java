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
import feign.httpclient.ApacheHttpClient;

import java.util.Timer;
import java.util.TimerTask;
import javax.annotation.PreDestroy;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientConnectionManagerFactory;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientFactory;
import org.springframework.cloud.netflix.feign.support.FeignHttpClientProperties;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Spencer Gibb
 */
@Configuration
@ConditionalOnClass(ApacheHttpClient.class)
@ConditionalOnProperty(value = "feign.httpclient.enabled", matchIfMissing = true)
class HttpClientFeignLoadBalancedConfiguration {

	@Configuration
	@ConditionalOnMissingBean(CloseableHttpClient.class)
	protected static class HttpClientFeignConfiguration {
		private final Timer connectionManagerTimer = new Timer(
				"FeignApacheHttpClientConfiguration.connectionManagerTimer", true);

		private CloseableHttpClient httpClient;

		@Autowired(required = false)
		private RegistryBuilder registryBuilder;

		@Bean
		@ConditionalOnMissingBean(HttpClientConnectionManager.class)
		public HttpClientConnectionManager connectionManager(
				ApacheHttpClientConnectionManagerFactory connectionManagerFactory,
				FeignHttpClientProperties httpClientProperties) {
			final HttpClientConnectionManager connectionManager = connectionManagerFactory
					.newConnectionManager(httpClientProperties.isDisableSslValidation(), httpClientProperties.getMaxConnections(),
							httpClientProperties.getMaxConnectionsPerRoute(),
							httpClientProperties.getTimeToLive(),
							httpClientProperties.getTimeToLiveUnit(), registryBuilder);
			this.connectionManagerTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					connectionManager.closeExpiredConnections();
				}
			}, 30000, httpClientProperties.getConnectionTimerRepeat());
			return connectionManager;
		}

		@Bean
		@ConditionalOnProperty(value = "feign.compression.response.enabled", havingValue = "true")
		public CloseableHttpClient customHttpClient(HttpClientConnectionManager httpClientConnectionManager,
											  FeignHttpClientProperties httpClientProperties) {
			HttpClientBuilder builder = HttpClientBuilder.create().disableCookieManagement().useSystemProperties();
			this.httpClient = createClient(builder, httpClientConnectionManager, httpClientProperties);
			return this.httpClient;
		}

		@Bean
		@ConditionalOnProperty(value = "feign.compression.response.enabled", havingValue = "false", matchIfMissing = true)
		public CloseableHttpClient httpClient(ApacheHttpClientFactory httpClientFactory, HttpClientConnectionManager httpClientConnectionManager,
											  FeignHttpClientProperties httpClientProperties) {
			this.httpClient = createClient(httpClientFactory.createBuilder(), httpClientConnectionManager, httpClientProperties);
			return this.httpClient;
		}

		private CloseableHttpClient createClient(HttpClientBuilder builder, HttpClientConnectionManager httpClientConnectionManager,
												 FeignHttpClientProperties httpClientProperties) {
			RequestConfig defaultRequestConfig = RequestConfig.custom()
					.setConnectTimeout(httpClientProperties.getConnectionTimeout())
					.setRedirectsEnabled(httpClientProperties.isFollowRedirects())
					.build();
			CloseableHttpClient httpClient = builder.setDefaultRequestConfig(defaultRequestConfig).
					setConnectionManager(httpClientConnectionManager).build();
			return httpClient;
		}

		@PreDestroy
		public void destroy() throws Exception {
			connectionManagerTimer.cancel();
			if(httpClient != null) {
				httpClient.close();
			}
		}
	}


		@Bean
		@ConditionalOnMissingBean(Client.class)
		public Client feignClient(CachingSpringLoadBalancerFactory cachingFactory,
								  SpringClientFactory clientFactory, HttpClient httpClient) {
			ApacheHttpClient delegate = new ApacheHttpClient(httpClient);
			return new LoadBalancerFeignClient(delegate, cachingFactory, clientFactory);
		}


}
