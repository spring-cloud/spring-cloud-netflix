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

package org.springframework.cloud.netflix.eureka;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.OrderedHealthAggregator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;

import lombok.extern.apachecommons.CommonsLog;

/**
 * @author Dave Syer
 * @author Spencer Gibb
 * @author Jon Schneider
 * @author Jakub Narloch
 */
@Configuration
@EnableConfigurationProperties
@ConditionalOnClass(EurekaClientConfig.class)
@ConditionalOnProperty(value = "eureka.client.enabled", matchIfMissing = true)
@CommonsLog
public class EurekaDiscoveryClientConfiguration implements SmartLifecycle, Ordered {

	private AtomicBoolean running = new AtomicBoolean(false);

	private int order = 0;

	private AtomicInteger port = new AtomicInteger(0);

	@Autowired
	private CloudEurekaInstanceConfig instanceConfig;

	@Autowired(required = false)
	private HealthCheckHandler healthCheckHandler;

	@Autowired
	private ApplicationContext context;

	@Autowired
	private ApplicationInfoManager applicationInfoManager;

	@Autowired
	private EurekaClient eurekaClient;

	@Override
	public void start() {
		// only set the port if the nonSecurePort is 0 and this.port != 0
		if (this.port.get() != 0 && this.instanceConfig.getNonSecurePort() == 0) {
			this.instanceConfig.setNonSecurePort(this.port.get());
		}

		// only initialize if nonSecurePort is greater than 0 and it isn't already running
		// because of containerPortInitializer below
		if (!this.running.get() && this.instanceConfig.getNonSecurePort() > 0) {

			maybeInitializeClient();

			if (log.isInfoEnabled()) {
				log.info("Registering application " + this.instanceConfig.getAppname()
						+ " with eureka with status "
						+ this.instanceConfig.getInitialStatus());
			}

			this.applicationInfoManager
					.setInstanceStatus(this.instanceConfig.getInitialStatus());

			if (this.healthCheckHandler != null) {
				this.eurekaClient.registerHealthCheck(this.healthCheckHandler);
			}
			this.context.publishEvent(
					new InstanceRegisteredEvent<>(this, this.instanceConfig));
			this.running.set(true);
		}
	}

	private void maybeInitializeClient() {
		// force initialization of possibly scoped proxies
		this.applicationInfoManager.getInfo();
		this.eurekaClient.getApplications();
	}

	@Override
	public void stop() {
		if (this.applicationInfoManager.getInfo() != null) {

			if (log.isInfoEnabled()) {
				log.info("Unregistering application " + this.instanceConfig.getAppname()
						+ " with eureka with status DOWN");
			}

			this.applicationInfoManager.setInstanceStatus(InstanceStatus.DOWN);
		}
		this.running.set(false);
	}

	@Override
	public boolean isRunning() {
		return this.running.get();
	}

	@Override
	public int getPhase() {
		return 0;
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public void stop(Runnable callback) {
		stop();
		callback.run();
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@EventListener(EmbeddedServletContainerInitializedEvent.class)
	public void onApplicationEvent(EmbeddedServletContainerInitializedEvent event) {
		// TODO: take SSL into account when Spring Boot 1.2 is available
		int localPort = event.getEmbeddedServletContainer().getPort();
		if (this.port.get() == 0) {
			log.info("Updating port to " + localPort);
			this.port.compareAndSet(0, localPort);
			start();
		}
	}

	@Configuration
	@ConditionalOnClass(RefreshScopeRefreshedEvent.class)
	protected static class EurekaClientConfigurationRefresher {
		@Autowired
		private EurekaDiscoveryClientConfiguration clientConfig;

		@EventListener(RefreshScopeRefreshedEvent.class)
		public void onApplicationEvent(RefreshScopeRefreshedEvent event) {
			// register in case meta data changed
			this.clientConfig.stop();
			this.clientConfig.start();
		}
	}

	@EventListener(ContextClosedEvent.class)
	public void onApplicationEvent(ContextClosedEvent event) {
		// register in case meta data changed
		stop();
		this.eurekaClient.shutdown();
	}

	@Configuration
	@ConditionalOnClass(Endpoint.class)
	@ConditionalOnProperty(name = "eureka.health.indicator.enabled", matchIfMissing = true)
	protected static class EurekaHealthIndicatorConfiguration {
		@Bean
		@ConditionalOnMissingBean
		public EurekaHealthIndicator eurekaHealthIndicator(EurekaClient eurekaClient,
				EurekaInstanceConfig instanceConfig, EurekaClientConfig clientConfig) {
			return new EurekaHealthIndicator(eurekaClient, instanceConfig, clientConfig);
		}
	}

	@Configuration
	@ConditionalOnClass(Endpoint.class)
	protected static class EurekaEndpointConfiguration {

		@Autowired(required = false)
		private EurekaHealthCheckHandler eurekaHealthCheckHandler;

		@Bean
		public EurekaEndpoint eurekaInstanceStatusEndpoint(ApplicationInfoManager applicationInfoManager) {
			return new EurekaEndpoint(applicationInfoManager, eurekaHealthCheckHandler);
		}
	}

	@Configuration
	@ConditionalOnProperty(value = "eureka.client.healthcheck.enabled", matchIfMissing = false)
	protected static class EurekaHealthCheckHandlerConfiguration {

		@Autowired(required = false)
		private HealthAggregator healthAggregator = new OrderedHealthAggregator();

		@Bean
		@ConditionalOnMissingBean(HealthCheckHandler.class)
		public EurekaHealthCheckHandler eurekaHealthCheckHandler() {
			return new EurekaHealthCheckHandler(this.healthAggregator);
		}
	}
}
