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

import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.commons.httpclient.OkHttpClientConnectionPoolFactory;
import org.springframework.cloud.commons.httpclient.OkHttpClientFactory;
import org.springframework.cloud.netflix.ribbon.RibbonClientName;
import org.springframework.cloud.netflix.ribbon.RibbonProperties;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerContext;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.client.AbstractLoadBalancerAwareClient;
import com.netflix.client.RetryHandler;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.servo.monitor.Monitors;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

/**
 * @author Spencer Gibb
 */
@Configuration
@ConditionalOnProperty("ribbon.okhttp.enabled")
@ConditionalOnClass(name = "okhttp3.OkHttpClient")
public class OkHttpRibbonConfiguration {
	@RibbonClientName
	private String name = "client";

	@Configuration
	protected static class OkHttpClientConfiguration {
		private OkHttpClient httpClient;

		@Bean
		@ConditionalOnMissingBean(ConnectionPool.class)
		public ConnectionPool httpClientConnectionPool(IClientConfig config,
													   OkHttpClientConnectionPoolFactory connectionPoolFactory) {
			RibbonProperties ribbon = RibbonProperties.from(config);
			int maxTotalConnections = ribbon.maxTotalConnections();
			long timeToLive = ribbon.poolKeepAliveTime();
			TimeUnit ttlUnit = ribbon.getPoolKeepAliveTimeUnits();
			return connectionPoolFactory.create(maxTotalConnections, timeToLive, ttlUnit);
		}

		@Bean
		@ConditionalOnMissingBean(OkHttpClient.class)
		public OkHttpClient client(OkHttpClientFactory httpClientFactory,
								   ConnectionPool connectionPool, IClientConfig config) {
			RibbonProperties ribbon = RibbonProperties.from(config);
			this.httpClient = httpClientFactory.createBuilder(false)
					.connectTimeout(ribbon.connectTimeout(), TimeUnit.MILLISECONDS)
					.readTimeout(ribbon.readTimeout(), TimeUnit.MILLISECONDS)
					.followRedirects(ribbon.isFollowRedirects())
					.connectionPool(connectionPool)
					.build();
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
	public RetryableOkHttpLoadBalancingClient retryableOkHttpLoadBalancingClient(
		IClientConfig config,
		ServerIntrospector serverIntrospector,
		ILoadBalancer loadBalancer,
		RetryHandler retryHandler,
		LoadBalancedRetryFactory loadBalancedRetryFactory,
		OkHttpClient delegate, RibbonLoadBalancerContext ribbonLoadBalancerContext) {
		RetryableOkHttpLoadBalancingClient client = new RetryableOkHttpLoadBalancingClient(delegate, config,
				serverIntrospector, loadBalancedRetryFactory);
		client.setLoadBalancer(loadBalancer);
		client.setRetryHandler(retryHandler);
		client.setRibbonLoadBalancerContext(ribbonLoadBalancerContext);
		Monitors.registerObject("Client_" + this.name, client);
		return client;
	}

	@Bean
	@ConditionalOnMissingBean(AbstractLoadBalancerAwareClient.class)
	@ConditionalOnMissingClass(value = "org.springframework.retry.support.RetryTemplate")
	public OkHttpLoadBalancingClient okHttpLoadBalancingClient(
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
