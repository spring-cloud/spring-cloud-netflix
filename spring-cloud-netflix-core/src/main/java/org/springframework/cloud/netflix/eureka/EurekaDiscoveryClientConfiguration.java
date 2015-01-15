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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.InstanceRegisteredEvent;
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
 *
 */
@Configuration
@EnableConfigurationProperties
@ConditionalOnClass(EurekaClientConfig.class)
@ConditionalOnProperty(value = "eureka.client.enabled", matchIfMissing = true)
public class EurekaDiscoveryClientConfiguration implements SmartLifecycle, Ordered {

	private static final Logger logger = LoggerFactory
			.getLogger(EurekaDiscoveryClientConfiguration.class);

	private AtomicBoolean running = new AtomicBoolean(false);

	private int order = 0;

	private AtomicInteger port = new AtomicInteger(0);

	@Autowired
	private EurekaInstanceConfigBean instanceConfig;

	@Autowired(required = false)
	private HealthCheckHandler healthCheckHandler;

	@Autowired
	private ApplicationContext context;

	@PreDestroy
	public void close() {
		closeDiscoveryClientJersey();
		logger.info("Removing application {} from eureka", instanceConfig.getAppname());
		DiscoveryManager.getInstance().shutdownComponent();
	}

	private void closeDiscoveryClientJersey() {
		logger.info("Closing DiscoveryClient.jerseyClient");
		Field jerseyClientField = ReflectionUtils.findField(
				com.netflix.discovery.DiscoveryClient.class, "discoveryJerseyClient",
				EurekaJerseyClient.JerseyClient.class);
		if (jerseyClientField != null) {
			try {
				jerseyClientField.setAccessible(true);
				Object obj = jerseyClientField.get(DiscoveryManager.getInstance()
						.getDiscoveryClient());
				if (obj != null) {
					EurekaJerseyClient.JerseyClient jerseyClient = (EurekaJerseyClient.JerseyClient) obj;
					jerseyClient.destroyResources();
				}
			}
			catch (Exception e) {
				logger.error("Error closing DiscoveryClient.jerseyClient", e);
			}
		}
	}

	@Override
	public void start() {
		// only set the port if the nonSecurePort is 0 and this.port != 0
		if (port.get() != 0 && instanceConfig.getNonSecurePort() == 0) {
			instanceConfig.setNonSecurePort(port.get());
		}
		// only initialize if nonSecurePort is greater than 0 and it isn't already running
		// because of containerPortInitializer below
		if (!running.get() && instanceConfig.getNonSecurePort() > 0) {
			discoveryManagerIntitializer().init();

			logger.info("Registering application {} with eureka with status {}",
					instanceConfig.getAppname(), instanceConfig.getInitialStatus());
			ApplicationInfoManager.getInstance().setInstanceStatus(
					instanceConfig.getInitialStatus());

			if (healthCheckHandler != null) {
				DiscoveryManager.getInstance().getDiscoveryClient()
						.registerHealthCheck(healthCheckHandler);
			}
			context.publishEvent(new InstanceRegisteredEvent<>(this, instanceConfig));
			running.set(true);
		}
	}

	@Override
	public void stop() {
		logger.info(
				"Unregistering application {} with eureka with status OUT_OF_SERVICE",
				instanceConfig.getAppname());
		ApplicationInfoManager.getInstance().setInstanceStatus(
				InstanceStatus.OUT_OF_SERVICE);
		running.set(false);
	}

	@Override
	public boolean isRunning() {
		return running.get();
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
		callback.run();
	}

	@Override
	public int getOrder() {
		return order;
	}

	@Bean
	@ConditionalOnMissingBean(DiscoveryManagerInitializer.class)
	public DiscoveryManagerInitializer discoveryManagerIntitializer() {
		return new DiscoveryManagerInitializer();
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
		@Bean
		@ConditionalOnMissingBean
		public EurekaHealthIndicator eurekaHealthIndicator(
				com.netflix.discovery.DiscoveryClient eurekaDiscoveryClient,
				MetricReader metrics, EurekaInstanceConfig config) {
			return new EurekaHealthIndicator(eurekaDiscoveryClient, metrics, config);
		}
	}
}
