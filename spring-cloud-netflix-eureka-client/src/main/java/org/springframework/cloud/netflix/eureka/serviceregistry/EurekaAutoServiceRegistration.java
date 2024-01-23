/*
 * Copyright 2013-2023 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.serviceregistry;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.cloud.client.discovery.event.InstancePreRegisteredEvent;
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.Ordered;

/**
 * Provides an implementation of {@link AutoServiceRegistration} for registering service
 * instances in Eureka.
 *
 * @author Dave Syer
 * @author Spencer Gibb
 * @author Jon Schneider
 * @author Jakub Narloch
 * @author Raiyan Raiyan
 * @author Olga Maciaszek-Sharma
 */
public class EurekaAutoServiceRegistration
		implements AutoServiceRegistration, SmartLifecycle, Ordered, SmartApplicationListener {

	private static final Log log = LogFactory.getLog(EurekaAutoServiceRegistration.class);

	private final AtomicBoolean running = new AtomicBoolean(false);

	private final int order = 0;

	private final AtomicInteger port = new AtomicInteger(0);

	private final ApplicationContext context;

	private final EurekaServiceRegistry serviceRegistry;

	private final EurekaRegistration registration;

	public EurekaAutoServiceRegistration(ApplicationContext context, EurekaServiceRegistry serviceRegistry,
			EurekaRegistration registration) {
		this.context = context;
		this.serviceRegistry = serviceRegistry;
		this.registration = registration;
	}

	@Override
	public void start() {
		// only set the port if the nonSecurePort or securePort is 0 and port != 0
		if (port.get() != 0) {
			if (registration.getNonSecurePort() == 0) {
				registration.setNonSecurePort(port.get());
			}

			if (registration.getSecurePort() == 0 && registration.isSecure()) {
				registration.setSecurePort(port.get());
			}
		}

		// only initialize if nonSecurePort is greater than 0 and it isn't already running
		// because of containerPortInitializer below
		if (!running.get() && registration.getNonSecurePort() > 0) {
			context.publishEvent(new InstancePreRegisteredEvent(this, registration));

			serviceRegistry.register(registration);

			context.publishEvent(new InstanceRegisteredEvent<>(this, registration.getInstanceConfig()));
			running.set(true);
		}
	}

	@Override
	public void stop() {
		serviceRegistry.deregister(registration);
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
		stop();
		callback.run();
	}

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return WebServerInitializedEvent.class.isAssignableFrom(eventType)
				|| ContextClosedEvent.class.isAssignableFrom(eventType);
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof WebServerInitializedEvent) {
			onApplicationEvent((WebServerInitializedEvent) event);
		}
		else if (event instanceof ContextClosedEvent) {
			onApplicationEvent((ContextClosedEvent) event);
		}
	}

	public void onApplicationEvent(WebServerInitializedEvent event) {
		// TODO: take SSL into account
		String contextName = event.getApplicationContext().getServerNamespace();
		if (contextName == null || !contextName.equals("management")) {
			int localPort = event.getWebServer().getPort();
			if (port.get() == 0) {
				log.info("Updating port to " + localPort);
				port.compareAndSet(0, localPort);
				start();
			}
		}
	}

	public void onApplicationEvent(ContextClosedEvent event) {
		if (event.getApplicationContext() == context) {
			stop();
		}
	}

}
