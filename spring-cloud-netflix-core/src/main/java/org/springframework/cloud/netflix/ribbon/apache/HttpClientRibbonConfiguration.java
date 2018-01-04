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

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.loadbalancer.LoadBalancedBackOffPolicyFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryListenerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicyFactory;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientConnectionManagerFactory;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientFactory;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.client.AbstractLoadBalancerAwareClient;
import com.netflix.client.RetryHandler;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
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

	@Configuration
	protected static class ApacheHttpClientConfiguration {
		private final Timer connectionManagerTimer = new Timer(
				"RibbonApacheHttpClientConfiguration.connectionManagerTimer", true);
		private CloseableHttpClient httpClient;

		@Autowired(required = false)
		private RegistryBuilder registryBuilder;

		@Bean
		@ConditionalOnMissingBean(HttpClientConnectionManager.class)
		public HttpClientConnectionManager httpClientConnectionManager(
				IClientConfig config,
				ApacheHttpClientConnectionManagerFactory connectionManagerFactory) {
			Integer maxTotalConnections = config.getPropertyAsInteger(
					CommonClientConfigKey.MaxTotalConnections,
					DefaultClientConfigImpl.DEFAULT_MAX_TOTAL_CONNECTIONS);
			Integer maxConnectionsPerHost = config.getPropertyAsInteger(
					CommonClientConfigKey.MaxConnectionsPerHost,
					DefaultClientConfigImpl.DEFAULT_MAX_CONNECTIONS_PER_HOST);
			Integer timerRepeat = config.getPropertyAsInteger(
					CommonClientConfigKey.ConnectionCleanerRepeatInterval,
					DefaultClientConfigImpl.DEFAULT_CONNECTION_IDLE_TIMERTASK_REPEAT_IN_MSECS);
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
			final HttpClientConnectionManager connectionManager = connectionManagerFactory
					.newConnectionManager(false, maxTotalConnections,
							maxConnectionsPerHost, timeToLive, ttlUnit, registryBuilder);
			this.connectionManagerTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					connectionManager.closeExpiredConnections();
				}
			}, 30000, timerRepeat);
			return connectionManager;
		}

		@Bean
		@ConditionalOnMissingBean(CloseableHttpClient.class)
		public CloseableHttpClient httpClient(ApacheHttpClientFactory httpClientFactory,
											  HttpClientConnectionManager connectionManager, IClientConfig config) {
			Boolean followRedirects = config.getPropertyAsBoolean(
					CommonClientConfigKey.FollowRedirects,
					DefaultClientConfigImpl.DEFAULT_FOLLOW_REDIRECTS);
			Integer connectTimeout = config.getPropertyAsInteger(
					CommonClientConfigKey.ConnectTimeout,
					DefaultClientConfigImpl.DEFAULT_CONNECT_TIMEOUT);
			RequestConfig defaultRequestConfig = RequestConfig.custom()
					.setConnectTimeout(connectTimeout)
					.setRedirectsEnabled(followRedirects).build();
			this.httpClient = httpClientFactory.createBuilder().
					setDefaultRequestConfig(defaultRequestConfig).
					setConnectionManager(connectionManager).build();
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
	@ConditionalOnMissingBean(AbstractLoadBalancerAwareClient.class)
	@ConditionalOnMissingClass(value = "org.springframework.retry.support.RetryTemplate")
	public RibbonLoadBalancingHttpClient ribbonLoadBalancingHttpClient(
		IClientConfig config, ServerIntrospector serverIntrospector,
		ILoadBalancer loadBalancer, RetryHandler retryHandler, CloseableHttpClient httpClient) {
		RibbonLoadBalancingHttpClient client = new RibbonLoadBalancingHttpClient(httpClient, config, serverIntrospector);
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
		LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory, CloseableHttpClient httpClient,
		LoadBalancedBackOffPolicyFactory loadBalancedBackOffPolicyFactory,
		LoadBalancedRetryListenerFactory loadBalancedRetryListenerFactory) {
		RetryableRibbonLoadBalancingHttpClient client = new RetryableRibbonLoadBalancingHttpClient(
			httpClient, config, serverIntrospector, loadBalancedRetryPolicyFactory,
			loadBalancedBackOffPolicyFactory, loadBalancedRetryListenerFactory);
		client.setLoadBalancer(loadBalancer);
		client.setRetryHandler(retryHandler);
		Monitors.registerObject("Client_" + this.name, client);
		return client;
	}
}
