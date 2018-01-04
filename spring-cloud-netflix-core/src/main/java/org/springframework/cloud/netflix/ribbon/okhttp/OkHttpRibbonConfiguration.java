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

package org.springframework.cloud.netflix.ribbon.okhttp;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import com.netflix.client.AbstractLoadBalancerAwareClient;
import com.netflix.client.RetryHandler;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.servo.monitor.Monitors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.loadbalancer.LoadBalancedBackOffPolicyFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryListenerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicyFactory;
import org.springframework.cloud.commons.httpclient.OkHttpClientConnectionPoolFactory;
import org.springframework.cloud.commons.httpclient.OkHttpClientFactory;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Spencer Gibb
 */
@Configuration
@ConditionalOnProperty("ribbon.okhttp.enabled")
@ConditionalOnClass(name = "okhttp3.OkHttpClient")
public class OkHttpRibbonConfiguration {
	@Value("${ribbon.client.name}")
	private String name = "client";

	@Configuration
	protected static class OkHttpClientConfiguration {
		private OkHttpClient httpClient;

		@Bean
		@ConditionalOnMissingBean(ConnectionPool.class)
		public ConnectionPool httpClientConnectionPool(IClientConfig config, OkHttpClientConnectionPoolFactory connectionPoolFactory) {
			Integer maxTotalConnections = config.getPropertyAsInteger(
					CommonClientConfigKey.MaxTotalConnections,
					DefaultClientConfigImpl.DEFAULT_MAX_TOTAL_CONNECTIONS);
			Object timeToLiveObj = config
					.getProperty(CommonClientConfigKey.PoolKeepAliveTime);
			Long timeToLive = DefaultClientConfigImpl.DEFAULT_POOL_KEEP_ALIVE_TIME;
			Object ttlUnitObj = config
					.getProperty(CommonClientConfigKey.PoolKeepAliveTimeUnits);
			TimeUnit ttlUnit = DefaultClientConfigImpl.DEFAULT_POOL_KEEP_ALIVE_TIME_UNITS;
			if (timeToLiveObj instanceof Long) {
				timeToLive = (Long) timeToLiveObj;
			}
			if (ttlUnitObj instanceof TimeUnit) {
				ttlUnit = (TimeUnit) ttlUnitObj;
			}
			return connectionPoolFactory.create(maxTotalConnections, timeToLive, ttlUnit);
		}

		@Bean
		@ConditionalOnMissingBean(OkHttpClient.class)
		public OkHttpClient client(OkHttpClientFactory httpClientFactory, ConnectionPool connectionPool, IClientConfig config) {
			Boolean followRedirects = config.getPropertyAsBoolean(
					CommonClientConfigKey.FollowRedirects,
					DefaultClientConfigImpl.DEFAULT_FOLLOW_REDIRECTS);
			Integer connectTimeout = config.getPropertyAsInteger(
					CommonClientConfigKey.ConnectTimeout,
					DefaultClientConfigImpl.DEFAULT_CONNECT_TIMEOUT);
			Integer readTimeout = config.getPropertyAsInteger(CommonClientConfigKey.ReadTimeout,
					DefaultClientConfigImpl.DEFAULT_READ_TIMEOUT);
			this.httpClient = httpClientFactory.createBuilder(false).
					connectTimeout(connectTimeout, TimeUnit.MILLISECONDS).
					readTimeout(readTimeout, TimeUnit.MILLISECONDS).
					followRedirects(followRedirects).
					connectionPool(connectionPool).build();
			return this.httpClient;
		}

		@PreDestroy
		public void destroy() {
			if(httpClient != null) {
				httpClient.dispatcher().executorService().shutdown();
				httpClient.connectionPool().evictAll();
			}
		}
	}

	@Bean
	@ConditionalOnMissingBean(AbstractLoadBalancerAwareClient.class)
	@ConditionalOnClass(name = "org.springframework.retry.support.RetryTemplate")
	public RetryableOkHttpLoadBalancingClient okHttpLoadBalancingClient(
		IClientConfig config,
		ServerIntrospector serverIntrospector,
		ILoadBalancer loadBalancer,
		RetryHandler retryHandler,
		LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory,
		OkHttpClient delegate,
		LoadBalancedBackOffPolicyFactory loadBalancedBackOffPolicyFactory,
		LoadBalancedRetryListenerFactory loadBalancedRetryListenerFactory) {
		RetryableOkHttpLoadBalancingClient client = new RetryableOkHttpLoadBalancingClient(delegate, config,
				serverIntrospector, loadBalancedRetryPolicyFactory, loadBalancedBackOffPolicyFactory, loadBalancedRetryListenerFactory);
		client.setLoadBalancer(loadBalancer);
		client.setRetryHandler(retryHandler);
		Monitors.registerObject("Client_" + this.name, client);
		return client;
	}

	@Bean
	@ConditionalOnMissingBean(AbstractLoadBalancerAwareClient.class)
	@ConditionalOnMissingClass(value = "org.springframework.retry.support.RetryTemplate")
	public OkHttpLoadBalancingClient retryableOkHttpLoadBalancingClient(
		IClientConfig config,
		ServerIntrospector serverIntrospector, ILoadBalancer loadBalancer,
		RetryHandler retryHandler, OkHttpClient delegate) {
		OkHttpLoadBalancingClient client = new OkHttpLoadBalancingClient(delegate, config,
				serverIntrospector);
		client.setLoadBalancer(loadBalancer);
		client.setRetryHandler(retryHandler);
		Monitors.registerObject("Client_" + this.name, client);
		return client;
	}
}
