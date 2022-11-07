/*
 * Copyright 2013-2022 the original author or authors.
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

package org.springframework.cloud.netflix.eureka;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.CompositeReactiveHealthContributor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.boot.actuate.health.ReactiveHealthContributor;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.cloud.client.discovery.health.DiscoveryCompositeHealthContributor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.Lifecycle;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

/**
 * A Eureka health checker, maps the application status into {@link InstanceStatus} that
 * will be propagated to Eureka registry.
 *
 * On each heartbeat Eureka performs the health check invoking registered
 * {@link HealthCheckHandler}. By default this implementation will perform aggregation of
 * all registered {@link HealthIndicator} through registered {@link StatusAggregator}.
 *
 * A {@code null} status is returned when the application context is closed (or in the
 * process of being closed). This prevents Eureka from updating the health status and only
 * consider the status present in the current InstanceInfo.
 *
 * @author Jakub Narloch
 * @author Spencer Gibb
 * @author Nowrin Anwar Joyita
 * @author Bertrand Renuart
 * @author Olga Maciaszek-Sharma
 * @see HealthCheckHandler
 * @see StatusAggregator
 */
public class EurekaHealthCheckHandler
		implements HealthCheckHandler, ApplicationContextAware, InitializingBean, Ordered, Lifecycle {

	private static final Map<Status, InstanceInfo.InstanceStatus> STATUS_MAPPING = new HashMap<>() {
		{
			put(Status.UNKNOWN, InstanceStatus.UNKNOWN);
			put(Status.OUT_OF_SERVICE, InstanceStatus.DOWN);
			put(Status.DOWN, InstanceStatus.DOWN);
			put(Status.UP, InstanceStatus.UP);
		}
	};

	private final StatusAggregator statusAggregator;

	private ApplicationContext applicationContext;

	private final Map<String, HealthContributor> healthContributors = new HashMap<>();

	/**
	 * {@code true} until the context is stopped.
	 */
	private boolean running = true;

	private final Map<String, ReactiveHealthContributor> reactiveHealthContributors = new HashMap<>();

	public EurekaHealthCheckHandler(StatusAggregator statusAggregator) {
		this.statusAggregator = statusAggregator;
		Assert.notNull(statusAggregator, "StatusAggregator must not be null");

	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() {
		populateHealthContributors(applicationContext.getBeansOfType(HealthContributor.class));
		reactiveHealthContributors.putAll(applicationContext.getBeansOfType(ReactiveHealthContributor.class));
	}

	void populateHealthContributors(Map<String, HealthContributor> healthContributors) {
		for (Map.Entry<String, HealthContributor> entry : healthContributors.entrySet()) {
			// ignore EurekaHealthIndicator and flatten the rest of the composite
			// otherwise there is a never ending cycle of down. See gh-643
			if (entry.getValue() instanceof DiscoveryCompositeHealthContributor indicator) {
				indicator.getIndicators().forEach((name, discoveryHealthIndicator) -> {
					if (!(discoveryHealthIndicator instanceof EurekaHealthIndicator)) {
						this.healthContributors.put(name, (HealthIndicator) discoveryHealthIndicator::health);
					}
				});
			}
			else {
				this.healthContributors.put(entry.getKey(), entry.getValue());
			}
		}
	}

	@Override
	public InstanceStatus getStatus(InstanceStatus instanceStatus) {
		if (running) {
			return getHealthStatus();
		}
		else {
			// Return nothing if the context is not running, so the status held by the
			// InstanceInfo remains unchanged.
			// See gh-1571
			return null;
		}
	}

	protected InstanceStatus getHealthStatus() {
		Status status = getStatus(statusAggregator);
		return mapToInstanceStatus(status);
	}

	protected Status getStatus(StatusAggregator statusAggregator) {
		Set<Status> statusSet = new HashSet<>();
		for (HealthContributor contributor : healthContributors.values()) {
			processContributor(statusSet, contributor);
		}
		for (ReactiveHealthContributor contributor : reactiveHealthContributors.values()) {
			processContributor(statusSet, contributor);
		}
		return statusAggregator.getAggregateStatus(statusSet);
	}

	private void processContributor(Set<Status> statusSet, HealthContributor contributor) {
		if (contributor instanceof CompositeHealthContributor) {
			for (NamedContributor<HealthContributor> contrib : (CompositeHealthContributor) contributor) {
				processContributor(statusSet, contrib.getContributor());
			}
		}
		else if (contributor instanceof HealthIndicator) {
			statusSet.add(((HealthIndicator) contributor).health().getStatus());
		}
	}

	private void processContributor(Set<Status> statusSet, ReactiveHealthContributor contributor) {
		if (contributor instanceof CompositeReactiveHealthContributor) {
			for (NamedContributor<ReactiveHealthContributor> contrib : (CompositeReactiveHealthContributor) contributor) {
				processContributor(statusSet, contrib.getContributor());
			}
		}
		else if (contributor instanceof ReactiveHealthIndicator) {
			Health health = ((ReactiveHealthIndicator) contributor).health().block();
			if (health != null) {
				statusSet.add(health.getStatus());
			}
		}
	}

	protected InstanceStatus mapToInstanceStatus(Status status) {
		if (!STATUS_MAPPING.containsKey(status)) {
			return InstanceStatus.UNKNOWN;
		}
		return STATUS_MAPPING.get(status);
	}

	@Override
	public int getOrder() {
		// registered with a high order priority so the close() method is invoked early
		// and *BEFORE* EurekaAutoServiceRegistration
		// (must be in effect when the registration is closed and the eureka replication
		// triggered -> health check handler is
		// consulted at that moment)
		return Ordered.HIGHEST_PRECEDENCE;
	}

	@Override
	public void start() {
		running = true;
	}

	@Override
	public void stop() {
		running = false;
	}

	@Override
	public boolean isRunning() {
		return true;
	}

}
