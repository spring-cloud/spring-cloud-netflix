/*
 * Copyright 2013-2019 the original author or authors.
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.appinfo.InstanceInfo;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.actuate.health.DefaultHealthIndicatorRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicatorRegistryFactory;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.cloud.client.discovery.health.DiscoveryCompositeHealthContributor;
import org.springframework.cloud.client.discovery.health.DiscoveryCompositeHealthIndicator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

import static com.netflix.appinfo.InstanceInfo.InstanceStatus;

/**
 * A Eureka health checker, maps the application status into {@link InstanceStatus} that
 * will be propagated to Eureka registry.
 *
 * On each heartbeat Eureka performs the health check invoking registered
 * {@link HealthCheckHandler}. By default this implementation will perform aggregation of
 * all registered {@link HealthIndicator} through registered {@link HealthAggregator}.
 *
 * @author Jakub Narloch
 * @author Spencer Gibb
 * @see HealthCheckHandler
 * @see StatusAggregator
 * @see HealthAggregator
 */
public class EurekaHealthCheckHandler
		implements HealthCheckHandler, ApplicationContextAware, InitializingBean {

	private static final Map<Status, InstanceInfo.InstanceStatus> STATUS_MAPPING = new HashMap<Status, InstanceInfo.InstanceStatus>() {
		{
			put(Status.UNKNOWN, InstanceStatus.UNKNOWN);
			put(Status.OUT_OF_SERVICE, InstanceStatus.OUT_OF_SERVICE);
			put(Status.DOWN, InstanceStatus.DOWN);
			put(Status.UP, InstanceStatus.UP);
		}
	};

	private StatusAggregator statusAggregator;

	private ApplicationContext applicationContext;

	private Map<String, HealthIndicator> healthIndicators;

	@Deprecated
	private CompositeHealthIndicator healthIndicator;

	@Deprecated
	private HealthIndicatorRegistryFactory healthIndicatorRegistryFactory;

	@Deprecated
	private HealthAggregator healthAggregator;

	@Deprecated
	public EurekaHealthCheckHandler(HealthAggregator healthAggregator) {
		Assert.notNull(healthAggregator, "HealthAggregator must not be null");
		this.healthAggregator = healthAggregator;
		this.healthIndicatorRegistryFactory = new HealthIndicatorRegistryFactory();
		this.healthIndicator = new CompositeHealthIndicator(this.healthAggregator,
				new DefaultHealthIndicatorRegistry());
	}

	public EurekaHealthCheckHandler(StatusAggregator statusAggregator) {
		this.statusAggregator = statusAggregator;
		Assert.notNull(statusAggregator, "StatusAggregator must not be null");

	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		final Map<String, HealthIndicator> healthIndicators = applicationContext
				.getBeansOfType(HealthIndicator.class);
		this.healthIndicators = new HashMap<>();

		if (statusAggregator != null) {
			populateHealthIndicators(healthIndicators);
		}
		else {
			createHealthIndicator(healthIndicators);
		}
	}

	@Deprecated
	void createHealthIndicator(Map<String, HealthIndicator> healthIndicators) {
		for (Map.Entry<String, HealthIndicator> entry : healthIndicators.entrySet()) {

			// ignore EurekaHealthIndicator and flatten the rest of the composite
			// otherwise there is a never ending cycle of down. See gh-643
			if (entry.getValue() instanceof DiscoveryCompositeHealthIndicator) {
				DiscoveryCompositeHealthIndicator indicator = (DiscoveryCompositeHealthIndicator) entry
						.getValue();
				for (DiscoveryCompositeHealthIndicator.Holder holder : indicator
						.getHealthIndicators()) {
					if (!(holder.getDelegate() instanceof EurekaHealthIndicator)) {
						this.healthIndicators.put(holder.getDelegate().getName(), holder);
					}
				}

			}
			else {
				this.healthIndicators.put(entry.getKey(), entry.getValue());
			}
		}
		this.healthIndicator = new CompositeHealthIndicator(healthAggregator,
				healthIndicatorRegistryFactory
						.createHealthIndicatorRegistry(this.healthIndicators));
	}

	void populateHealthIndicators(Map<String, HealthIndicator> healthIndicators) {
		for (Map.Entry<String, HealthIndicator> entry : healthIndicators.entrySet()) {
			// ignore EurekaHealthIndicator and flatten the rest of the composite
			// otherwise there is a never ending cycle of down. See gh-643
			if (entry.getValue() instanceof DiscoveryCompositeHealthContributor) {
				DiscoveryCompositeHealthContributor indicator = (DiscoveryCompositeHealthContributor) entry
						.getValue();
				indicator.forEach(contributor -> {
					if (!(contributor
							.getContributor() instanceof EurekaHealthIndicator)) {
						this.healthIndicators.put(contributor.getName(),
								(HealthIndicator) contributor.getContributor());
					}
				});
			}
			else {
				this.healthIndicators.put(entry.getKey(), entry.getValue());
			}
		}
	}

	@Override
	public InstanceStatus getStatus(InstanceStatus instanceStatus) {
		return getHealthStatus();
	}

	protected InstanceStatus getHealthStatus() {
		final Status status;
		if (statusAggregator != null) {
			status = getStatus(statusAggregator);
		}
		else {
			status = getStatus(getHealthIndicator());
		}
		return mapToInstanceStatus(status);
	}

	@Deprecated
	private Status getStatus(CompositeHealthIndicator healthIndicator) {
		Status status;
		status = healthIndicator.health().getStatus();
		return status;
	}

	protected Status getStatus(StatusAggregator statusAggregator) {
		Status status;
		Set<Status> statusSet = healthIndicators.values().stream()
				.map(HealthIndicator::health).map(Health::getStatus)
				.collect(Collectors.toSet());
		status = statusAggregator.getAggregateStatus(statusSet);
		return status;
	}

	protected InstanceStatus mapToInstanceStatus(Status status) {
		if (!STATUS_MAPPING.containsKey(status)) {
			return InstanceStatus.UNKNOWN;
		}
		return STATUS_MAPPING.get(status);
	}

	@Deprecated
	protected CompositeHealthIndicator getHealthIndicator() {
		return healthIndicator;
	}

}
