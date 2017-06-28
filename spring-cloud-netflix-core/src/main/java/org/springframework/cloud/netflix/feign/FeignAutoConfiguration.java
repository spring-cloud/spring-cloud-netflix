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

package org.springframework.cloud.netflix.feign;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientConnectionManagerFactory;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientFactory;
import org.springframework.cloud.netflix.feign.support.FeignHttpClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.Client;
import feign.Feign;
import feign.httpclient.ApacheHttpClient;
import feign.okhttp.OkHttpClient;

import javax.annotation.PreDestroy;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;

/**
 * @author Spencer Gibb
 * @author Julien Roy
 */
@Configuration
@ConditionalOnClass(Feign.class)
@EnableConfigurationProperties({FeignHttpClientProperties.class})
public class FeignAutoConfiguration {

	@Autowired(required = false)
	private List<FeignClientSpecification> configurations = new ArrayList<>();

	@Bean
	public HasFeatures feignFeature() {
		return HasFeatures.namedFeature("Feign", Feign.class);
	}

	@Bean
	public FeignContext feignContext() {
		FeignContext context = new FeignContext();
		context.setConfigurations(this.configurations);
		return context;
	}

	@Configuration
	@ConditionalOnClass(name = "feign.hystrix.HystrixFeign")
	protected static class HystrixFeignTargeterConfiguration {
		@Bean
		@ConditionalOnMissingBean
		public Targeter feignTargeter() {
			return new HystrixTargeter();
		}
	}

	@Configuration
	@ConditionalOnMissingClass("feign.hystrix.HystrixFeign")
	protected static class DefaultFeignTargeterConfiguration {
		@Bean
		@ConditionalOnMissingBean
		public Targeter feignTargeter() {
			return new DefaultTargeter();
		}
	}

	// the following configuration is for alternate feign clients if
	// ribbon is not on the class path.
	// see corresponding configurations in FeignRibbonClientAutoConfiguration
	// for load balanced ribbon clients.
	@Configuration
	@ConditionalOnClass(ApacheHttpClient.class)
	@ConditionalOnMissingClass("com.netflix.loadbalancer.ILoadBalancer")
	@ConditionalOnProperty(value = "feign.httpclient.enabled", matchIfMissing = true)
	protected static class HttpClientFeignConfiguration {
		private final Timer connectionManagerTimer = new Timer(
				"FeignApacheHttpClientConfiguration.connectionManagerTimer", true);

		@Autowired(required = false)
		private RegistryBuilder registryBuilder;

		private CloseableHttpClient httpClient;

		@Bean
		public HttpClientConnectionManager connectionManager(ApacheHttpClientConnectionManagerFactory connectionManagerFactory,
															 FeignHttpClientProperties httpClientProperties) {
			final HttpClientConnectionManager connectionManager = connectionManagerFactory.newConnectionManager(false, httpClientProperties.getMaxConnections(),
					httpClientProperties.getMaxConnectionsPerRoute(), httpClientProperties.getTimeToLive(),
					httpClientProperties.getTimeToLiveUnit(),
					registryBuilder);
			this.connectionManagerTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					connectionManager.closeExpiredConnections();
				}
			}, 30000, httpClientProperties.getConnectionTimerRepeat());
			return connectionManager;
		}

		@Bean
		public CloseableHttpClient httpClient(ApacheHttpClientFactory httpClientFactory, HttpClientConnectionManager httpClientConnectionManager,
											  FeignHttpClientProperties httpClientProperties) {
			RequestConfig defaultRequestConfig = RequestConfig.custom().
					setConnectTimeout(httpClientProperties.getConnectionTimeout()).
					setRedirectsEnabled(httpClientProperties.isFollowRedirects()).
					build();
			this.httpClient = httpClientFactory.createClient(defaultRequestConfig, httpClientConnectionManager);
			return this.httpClient;
		}

		@Bean
		@ConditionalOnMissingBean(Client.class)
		public Client feignClient(HttpClient httpClient) {
			return new ApacheHttpClient(httpClient);
		}

		@PreDestroy
		public void destroy() throws Exception {
			connectionManagerTimer.cancel();
			httpClient.close();
		}
	}

	@Configuration
	@ConditionalOnClass(OkHttpClient.class)
	@ConditionalOnMissingClass("com.netflix.loadbalancer.ILoadBalancer")
	@ConditionalOnProperty(value = "feign.okhttp.enabled", matchIfMissing = true)
	protected static class OkHttpFeignConfiguration {

		@Autowired(required = false)
		private okhttp3.OkHttpClient okHttpClient;

		@Bean
		@ConditionalOnMissingBean(Client.class)
		public Client feignClient() {
			if (this.okHttpClient != null) {
				return new OkHttpClient(this.okHttpClient);
			}
			return new OkHttpClient();
		}
	}

}
