/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.cloud.netflix.ribbon;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicyFactory;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientConnectionManagerFactory;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientFactory;
import org.springframework.cloud.commons.httpclient.HttpClientConfiguration;
import org.springframework.cloud.commons.httpclient.OkHttpClientConnectionPoolFactory;
import org.springframework.cloud.commons.httpclient.OkHttpClientFactory;
import org.springframework.cloud.netflix.ribbon.apache.RetryableRibbonLoadBalancingHttpClient;
import org.springframework.cloud.netflix.ribbon.apache.RibbonLoadBalancingHttpClient;
import org.springframework.cloud.netflix.ribbon.okhttp.OkHttpLoadBalancingClient;
import org.springframework.cloud.netflix.ribbon.okhttp.RetryableOkHttpLoadBalancingClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

import com.netflix.client.AbstractLoadBalancerAwareClient;
import com.netflix.client.DefaultLoadBalancerRetryHandler;
import com.netflix.client.RetryHandler;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ConfigurationBasedServerList;
import com.netflix.loadbalancer.DummyPing;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.PollingServerListUpdater;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.loadbalancer.ServerListFilter;
import com.netflix.loadbalancer.ServerListUpdater;
import com.netflix.loadbalancer.ZoneAvoidanceRule;
import com.netflix.loadbalancer.ZoneAwareLoadBalancer;
import com.netflix.niws.client.http.RestClient;
import com.netflix.servo.monitor.Monitors;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.client.apache4.ApacheHttpClient4;

import static com.netflix.client.config.CommonClientConfigKey.DeploymentContextBasedVipAddresses;
import static org.springframework.cloud.netflix.ribbon.RibbonUtils.setRibbonProperty;
import static org.springframework.cloud.netflix.ribbon.RibbonUtils.updateToHttpsIfNeeded;

/**
 * @author Dave Syer
 */
@SuppressWarnings("deprecation")
@Configuration
@EnableConfigurationProperties
@Import(HttpClientConfiguration.class)
public class RibbonClientConfiguration {

	@Value("${ribbon.client.name}")
	private String name = "client";

	// TODO: maybe re-instate autowired load balancers: identified by name they could be
	// associated with ribbon clients

	@Autowired
	private PropertiesFactory propertiesFactory;

	@Bean
	@ConditionalOnMissingBean
	public IClientConfig ribbonClientConfig() {
		DefaultClientConfigImpl config = new DefaultClientConfigImpl();
		config.loadProperties(this.name);
		return config;
	}

	@Bean
	@ConditionalOnMissingBean
	public IRule ribbonRule(IClientConfig config) {
		if (this.propertiesFactory.isSet(IRule.class, name)) {
			return this.propertiesFactory.get(IRule.class, config, name);
		}
		ZoneAvoidanceRule rule = new ZoneAvoidanceRule();
		rule.initWithNiwsConfig(config);
		return rule;
	}

	@Bean
	@ConditionalOnMissingBean
	public IPing ribbonPing(IClientConfig config) {
		if (this.propertiesFactory.isSet(IPing.class, name)) {
			return this.propertiesFactory.get(IPing.class, config, name);
		}
		return new DummyPing();
	}

	@Bean
	@ConditionalOnMissingBean
	@SuppressWarnings("unchecked")
	public ServerList<Server> ribbonServerList(IClientConfig config) {
		if (this.propertiesFactory.isSet(ServerList.class, name)) {
			return this.propertiesFactory.get(ServerList.class, config, name);
		}
		ConfigurationBasedServerList serverList = new ConfigurationBasedServerList();
		serverList.initWithNiwsConfig(config);
		return serverList;
	}

	@Configuration
	@ConditionalOnProperty(name = "spring.cloud.httpclient.apache.enable", matchIfMissing = true)
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
				ApacheHttpClientConnectionManagerFactory connectionManagerFactory,
				ApacheHttpClientFactory httpClientFactory) {
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
			this.httpClient = httpClientFactory.createClient(defaultRequestConfig,
					connectionManager);
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

	@Configuration
	@ConditionalOnProperty("spring.cloud.httpclient.ok.enabled")
	@ConditionalOnClass(name = "okhttp3.OkHttpClient")
	protected static class OkHttpClientConfiguration {
		private OkHttpClient httpClient;

		@Bean
		@ConditionalOnMissingBean(ConnectionPool.class)
		public ConnectionPool httpClientConnectionPool(IClientConfig config,
				OkHttpClientConnectionPoolFactory connectionPoolFactory) {
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
		public OkHttpClient client(OkHttpClientFactory httpClientFactory,
				ConnectionPool connectionPool, IClientConfig config) {
			Boolean followRedirects = config.getPropertyAsBoolean(
					CommonClientConfigKey.FollowRedirects,
					DefaultClientConfigImpl.DEFAULT_FOLLOW_REDIRECTS);
			Integer connectTimeout = config.getPropertyAsInteger(
					CommonClientConfigKey.ConnectTimeout,
					DefaultClientConfigImpl.DEFAULT_CONNECT_TIMEOUT);
			Integer readTimeout = config.getPropertyAsInteger(CommonClientConfigKey.ReadTimeout,
					DefaultClientConfigImpl.DEFAULT_READ_TIMEOUT);
			this.httpClient = httpClientFactory.create(false, connectTimeout, TimeUnit.MILLISECONDS,
					followRedirects, readTimeout, TimeUnit.MILLISECONDS, connectionPool, null,
					null);
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

	@Configuration
	@ConditionalOnProperty(name = "spring.cloud.httpclient.apache.enable", matchIfMissing = true)
	protected static class HttpClientRibbonConfiguration {
		@Value("${ribbon.client.name}")
		private String name = "client";

		@Bean
		@ConditionalOnMissingBean(AbstractLoadBalancerAwareClient.class)
		@ConditionalOnMissingClass(value = "org.springframework.retry.support.RetryTemplate")
		public RibbonLoadBalancingHttpClient ribbonLoadBalancingHttpClient(
				IClientConfig config, ServerIntrospector serverIntrospector,
				ILoadBalancer loadBalancer, RetryHandler retryHandler,
				CloseableHttpClient httpClient) {
			RibbonLoadBalancingHttpClient client = new RibbonLoadBalancingHttpClient(
					httpClient, config, serverIntrospector);
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
				LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory,
				CloseableHttpClient httpClient) {
			RetryableRibbonLoadBalancingHttpClient client = new RetryableRibbonLoadBalancingHttpClient(
					httpClient, config, serverIntrospector,
					loadBalancedRetryPolicyFactory);
			client.setLoadBalancer(loadBalancer);
			client.setRetryHandler(retryHandler);
			Monitors.registerObject("Client_" + this.name, client);
			return client;
		}
	}

	@Configuration
	@ConditionalOnProperty("spring.cloud.httpclient.ok.enabled")
	@ConditionalOnClass(name = "okhttp3.OkHttpClient")
	protected static class OkHttpRibbonConfiguration {
		@Value("${ribbon.client.name}")
		private String name = "client";

		@Bean
		@ConditionalOnMissingBean(AbstractLoadBalancerAwareClient.class)
		@ConditionalOnClass(name = "org.springframework.retry.support.RetryTemplate")
		public RetryableOkHttpLoadBalancingClient okHttpLoadBalancingClient(
				IClientConfig config, ServerIntrospector serverIntrospector,
				ILoadBalancer loadBalancer, RetryHandler retryHandler,
				LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory,
				OkHttpClient delegate) {
			RetryableOkHttpLoadBalancingClient client = new RetryableOkHttpLoadBalancingClient(
					delegate, config, serverIntrospector, loadBalancedRetryPolicyFactory);
			client.setLoadBalancer(loadBalancer);
			client.setRetryHandler(retryHandler);
			Monitors.registerObject("Client_" + this.name, client);
			return client;
		}

		@Bean
		@ConditionalOnMissingBean(AbstractLoadBalancerAwareClient.class)
		@ConditionalOnMissingClass(value = "org.springframework.retry.support.RetryTemplate")
		public OkHttpLoadBalancingClient retryableOkHttpLoadBalancingClient(
				IClientConfig config, ServerIntrospector serverIntrospector,
				ILoadBalancer loadBalancer, RetryHandler retryHandler, OkHttpClient delegate) {
			OkHttpLoadBalancingClient client = new OkHttpLoadBalancingClient(delegate, config,
					serverIntrospector);
			client.setLoadBalancer(loadBalancer);
			client.setRetryHandler(retryHandler);
			Monitors.registerObject("Client_" + this.name, client);
			return client;
		}
	}

	@Configuration
	@RibbonAutoConfiguration.ConditionalOnRibbonRestClient
	protected static class RestClientRibbonConfiguration {
		@Value("${ribbon.client.name}")
		private String name = "client";

		/**
		 * Create a Netflix {@link RestClient} integrated with Ribbon if none already
		 * exists in the application context. It is not required for Ribbon to work
		 * properly and is therefore created lazily if ever another component requires it.
		 *
		 * @param config the configuration to use by the underlying Ribbon instance
		 * @param loadBalancer the load balancer to use by the underlying Ribbon instance
		 * @param serverIntrospector server introspector to use by the underlying Ribbon
		 * instance
		 * @param retryHandler retry handler to use by the underlying Ribbon instance
		 * @return a {@link RestClient} instances backed by Ribbon
		 */
		@Bean
		@Lazy
		@ConditionalOnMissingBean(AbstractLoadBalancerAwareClient.class)
		public RestClient ribbonRestClient(IClientConfig config,
				ILoadBalancer loadBalancer, ServerIntrospector serverIntrospector,
				RetryHandler retryHandler) {
			RestClient client = new OverrideRestClient(config, serverIntrospector);
			client.setLoadBalancer(loadBalancer);
			client.setRetryHandler(retryHandler);
			Monitors.registerObject("Client_" + this.name, client);
			return client;
		}
	}

	@Bean
	@ConditionalOnMissingBean
	public ServerListUpdater ribbonServerListUpdater(IClientConfig config) {
		return new PollingServerListUpdater(config);
	}

	@Bean
	@ConditionalOnMissingBean
	public ILoadBalancer ribbonLoadBalancer(IClientConfig config,
			ServerList<Server> serverList, ServerListFilter<Server> serverListFilter,
			IRule rule, IPing ping, ServerListUpdater serverListUpdater) {
		if (this.propertiesFactory.isSet(ILoadBalancer.class, name)) {
			return this.propertiesFactory.get(ILoadBalancer.class, config, name);
		}
		return new ZoneAwareLoadBalancer<>(config, rule, ping, serverList,
				serverListFilter, serverListUpdater);
	}

	@Bean
	@ConditionalOnMissingBean
	@SuppressWarnings("unchecked")
	public ServerListFilter<Server> ribbonServerListFilter(IClientConfig config) {
		if (this.propertiesFactory.isSet(ServerListFilter.class, name)) {
			return this.propertiesFactory.get(ServerListFilter.class, config, name);
		}
		ZonePreferenceServerListFilter filter = new ZonePreferenceServerListFilter();
		filter.initWithNiwsConfig(config);
		return filter;
	}

	@Bean
	@ConditionalOnMissingBean
	public RibbonLoadBalancerContext ribbonLoadBalancerContext(ILoadBalancer loadBalancer,
			IClientConfig config, RetryHandler retryHandler) {
		return new RibbonLoadBalancerContext(loadBalancer, config, retryHandler);
	}

	@Bean
	@ConditionalOnMissingBean
	public RetryHandler retryHandler(IClientConfig config) {
		return new DefaultLoadBalancerRetryHandler(config);
	}

	@Bean
	@ConditionalOnMissingBean
	public ServerIntrospector serverIntrospector() {
		return new DefaultServerIntrospector();
	}

	@PostConstruct
	public void preprocess() {
		setRibbonProperty(name, DeploymentContextBasedVipAddresses.key(), name);
	}

	static class OverrideRestClient extends RestClient {

		private IClientConfig config;
		private ServerIntrospector serverIntrospector;

		protected OverrideRestClient(IClientConfig config,
				ServerIntrospector serverIntrospector) {
			super();
			this.config = config;
			this.serverIntrospector = serverIntrospector;
			initWithNiwsConfig(this.config);
		}

		@Override
		public URI reconstructURIWithServer(Server server, URI original) {
			URI uri = updateToHttpsIfNeeded(original, this.config,
					this.serverIntrospector, server);
			return super.reconstructURIWithServer(server, uri);
		}

		@Override
		protected Client apacheHttpClientSpecificInitialization() {
			ApacheHttpClient4 apache = (ApacheHttpClient4) super.apacheHttpClientSpecificInitialization();
			apache.getClientHandler().getHttpClient().getParams().setParameter(
					ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES);
			return apache;
		}

	}

}
