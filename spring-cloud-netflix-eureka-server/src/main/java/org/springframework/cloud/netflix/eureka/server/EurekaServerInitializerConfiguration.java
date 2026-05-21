/*
 * Copyright 2013-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.eureka.server;

import java.util.concurrent.CompletableFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.netflix.eureka.server.event.EurekaRegistryAvailableEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaServerStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import com.netflix.eureka.EurekaServerConfig;

import jakarta.servlet.ServletContext;

/**
 * @author Dave Syer
 */
@Configuration(proxyBeanMethods = false)
public class EurekaServerInitializerConfiguration implements SmartLifecycle, Ordered {

	private static final Log log = LogFactory.getLog(EurekaServerInitializerConfiguration.class);

	private static final int ORDER = 1;

	private final EurekaServerConfig eurekaServerConfig;

	private final ServletContext servletContext;

	private final ApplicationContext applicationContext;

	private final EurekaServerBootstrap eurekaServerBootstrap;

	private volatile boolean running;

	public EurekaServerInitializerConfiguration(EurekaServerConfig eurekaServerConfig, ServletContext servletContext,
			ApplicationContext applicationContext, EurekaServerBootstrap eurekaServerBootstrap) {
		this.eurekaServerConfig = eurekaServerConfig;
		this.servletContext = servletContext;
		this.applicationContext = applicationContext;
		this.eurekaServerBootstrap = eurekaServerBootstrap;
	}

	@Override
	public void start() {
		CompletableFuture.runAsync(() -> {
			try {
				// TODO: is this class even needed now?
				eurekaServerBootstrap.contextInitialized(EurekaServerInitializerConfiguration.this.servletContext);
				log.info("Started Eureka Server");

				publish(new EurekaRegistryAvailableEvent(getEurekaServerConfig()));
				EurekaServerInitializerConfiguration.this.running = true;
				publish(new EurekaServerStartedEvent(getEurekaServerConfig()));
			}
			catch (Exception ex) {
				// Help!
				log.error("Could not initialize Eureka servlet context", ex);
			}
		});
	}

	private EurekaServerConfig getEurekaServerConfig() {
		return this.eurekaServerConfig;
	}

	private void publish(ApplicationEvent event) {
		this.applicationContext.publishEvent(event);
	}

	@Override
	public void stop() {
		this.running = false;
		eurekaServerBootstrap.contextDestroyed(this.servletContext);
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public int getPhase() {
		return 0;
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

}
