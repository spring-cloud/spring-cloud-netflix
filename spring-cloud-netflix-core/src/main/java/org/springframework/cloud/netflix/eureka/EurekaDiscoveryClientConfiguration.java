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

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PreDestroy;

import lombok.extern.apachecommons.CommonsLog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.metrics.reader.CompositeMetricReader;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.Ordered;
import org.springframework.util.ReflectionUtils;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.discovery.DiscoveryManager;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.EurekaJerseyClient;

/**
 * @author Dave Syer
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
	private EurekaInstanceConfigBean instanceConfig;

	@Autowired(required = false)
	private HealthCheckHandler healthCheckHandler;

	@Autowired
	private DiscoveryManagerInitializer discoveryManagerInitializer;

	@Autowired
	private ApplicationContext context;

	@PreDestroy
	public void close() {
		closeDiscoveryClientJersey();
		log.info("Removing application " + this.instanceConfig.getAppname()
				+ " from eureka");
		DiscoveryManager.getInstance().shutdownComponent();
	}

	private void closeDiscoveryClientJersey() {
		log.info("Closing DiscoveryClient.jerseyClient");
		Field jerseyClientField = ReflectionUtils.findField(
				com.netflix.discovery.DiscoveryClient.class, "discoveryJerseyClient",
				EurekaJerseyClient.JerseyClient.class);
		if (jerseyClientField != null) {
			try {
				jerseyClientField.setAccessible(true);
				if (DiscoveryManager.getInstance() != null
						&& DiscoveryManager.getInstance().getDiscoveryClient() != null) {
					Object obj = jerseyClientField.get(DiscoveryManager.getInstance()
							.getDiscoveryClient());
					if (obj != null) {
						EurekaJerseyClient.JerseyClient jerseyClient = (EurekaJerseyClient.JerseyClient) obj;
						jerseyClient.destroyResources();
					}
				}
			}
			catch (Exception ex) {
				log.error("Error closing DiscoveryClient.jerseyClient", ex);
			}
		}
	}

	@Override
	public void start() {
		// only set the port if the nonSecurePort is 0 and this.port != 0
		if (this.port.get() != 0 && this.instanceConfig.getNonSecurePort() == 0) {
			this.instanceConfig.setNonSecurePort(this.port.get());
		}
		// only initialize if nonSecurePort is greater than 0 and it isn't already running
		// because of containerPortInitializer below
		if (!this.running.get() && this.instanceConfig.getNonSecurePort() > 0) {
			this.discoveryManagerInitializer.init();

			log.info("Registering application " + this.instanceConfig.getAppname()
					+ " with eureka with status "
					+ this.instanceConfig.getInitialStatus());

			ApplicationInfoManager.getInstance().setInstanceStatus(
					this.instanceConfig.getInitialStatus());

			if (this.healthCheckHandler != null) {
				DiscoveryManager.getInstance().getDiscoveryClient()
						.registerHealthCheck(this.healthCheckHandler);
			}
			this.context.publishEvent(new InstanceRegisteredEvent<>(this,
					this.instanceConfig));
			this.running.set(true);
		}
	}

	@Override
	public void stop() {
		log.info("Unregistering application " + this.instanceConfig.getAppname()
				+ " with eureka with status OUT_OF_SERVICE");
		if (ApplicationInfoManager.getInstance().getInfo() != null) {
			ApplicationInfoManager.getInstance().setInstanceStatus(
					InstanceStatus.OUT_OF_SERVICE);
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

	@Configuration
	protected static class DiscoveryManagerInitializerConfiguration {

		@Bean
		@ConditionalOnMissingBean(DiscoveryManagerInitializer.class)
		public DiscoveryManagerInitializer discoveryManagerInitializer() {
			return new DiscoveryManagerInitializer();
		}

	}

	@Bean
	@Lazy
	@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
	@ConditionalOnMissingBean(com.netflix.discovery.DiscoveryClient.class)
	public com.netflix.discovery.DiscoveryClient eurekaDiscoveryClient() {
		return DiscoveryManager.getInstance().getDiscoveryClient();
	}

	@Bean
	public DiscoveryClient discoveryClient() {
		return new EurekaDiscoveryClient();
	}

	@Bean
	protected ApplicationListener<EmbeddedServletContainerInitializedEvent> containerPortInitializer() {
		return new ApplicationListener<EmbeddedServletContainerInitializedEvent>() {

			@Override
			public void onApplicationEvent(EmbeddedServletContainerInitializedEvent event) {
				// TODO: take SSL into account when Spring Boot 1.2 is available
				EurekaDiscoveryClientConfiguration.this.port.compareAndSet(0, event
						.getEmbeddedServletContainer().getPort());
				EurekaDiscoveryClientConfiguration.this.start();
			}
		};
	}

	@Configuration
	@ConditionalOnClass(Endpoint.class)
	@ConditionalOnBean(MetricReader.class)
	protected static class EurekaHealthIndicatorConfiguration {
		
		@Autowired
		private List<MetricReader> metricReaders = Collections.emptyList();
		
		@Bean
		@ConditionalOnMissingBean
		public EurekaHealthIndicator eurekaHealthIndicator(
				com.netflix.discovery.DiscoveryClient eurekaDiscoveryClient, EurekaInstanceConfig config) {
			CompositeMetricReader metrics = new CompositeMetricReader(metricReaders.toArray(new MetricReader[0]));
			return new EurekaHealthIndicator(eurekaDiscoveryClient, metrics, config);
		}
	}

}
