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
 */

package org.springframework.cloud.netflix.eureka;

import java.util.HashMap;
import java.util.Map;

import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.appinfo.InstanceInfo;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.discovery.health.DiscoveryCompositeHealthIndicator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

import static com.netflix.appinfo.InstanceInfo.InstanceStatus;

/**
 * A Eureka health checker, maps the application status into {@link InstanceStatus}
 * that will be propagated to Eureka registry.
 *
 * On each heartbeat Eureka performs the health check invoking registered {@link HealthCheckHandler}. By default this
 * implementation will perform aggregation of all registered {@link HealthIndicator}
 * through registered {@link HealthAggregator}.
 *
 * @author Jakub Narloch
 * @see HealthCheckHandler
 * @see HealthAggregator
 */
public class EurekaHealthCheckHandler implements HealthCheckHandler, ApplicationContextAware, InitializingBean {

	private static final Map<Status, InstanceInfo.InstanceStatus> STATUS_MAPPING =
			new HashMap<Status, InstanceInfo.InstanceStatus>() {{
				put(Status.UNKNOWN, InstanceStatus.UNKNOWN);
				put(Status.OUT_OF_SERVICE, InstanceStatus.OUT_OF_SERVICE);
				put(Status.DOWN, InstanceStatus.DOWN);
				put(Status.UP, InstanceStatus.UP);
			}};

	private final CompositeHealthIndicator healthIndicator;

	private ApplicationContext applicationContext;

	public EurekaHealthCheckHandler(HealthAggregator healthAggregator) {
		Assert.notNull(healthAggregator, "HealthAggregator must not be null");
		this.healthIndicator = new CompositeHealthIndicator(healthAggregator);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		final Map<String, HealthIndicator> healthIndicators = applicationContext.getBeansOfType(HealthIndicator.class);

		for (Map.Entry<String, HealthIndicator> entry : healthIndicators.entrySet()) {

			//ignore EurekaHealthIndicator and flatten the rest of the composite
			//otherwise there is a never ending cycle of down. See gh-643
			if (entry.getValue() instanceof DiscoveryCompositeHealthIndicator) {
				DiscoveryCompositeHealthIndicator indicator = (DiscoveryCompositeHealthIndicator) entry.getValue();
				for (DiscoveryCompositeHealthIndicator.Holder holder : indicator.getHealthIndicators()) {
					if (!(holder.getDelegate() instanceof EurekaHealthIndicator)) {
						healthIndicator.addHealthIndicator(holder.getDelegate().getName(), holder);
					}
				}

			}
			else {
				healthIndicator.addHealthIndicator(entry.getKey(), entry.getValue());
			}
		}
	}

	@Override
	public InstanceStatus getStatus(InstanceStatus instanceStatus) {
		return getHealthStatus();
	}

	protected InstanceStatus getHealthStatus() {
		final Status status = healthIndicator.health().getStatus();
		return mapToInstanceStatus(status);
	}

	protected InstanceStatus mapToInstanceStatus(Status status) {
		if (!STATUS_MAPPING.containsKey(status)) {
			return InstanceStatus.UNKNOWN;
		}
		return STATUS_MAPPING.get(status);
	}
}
