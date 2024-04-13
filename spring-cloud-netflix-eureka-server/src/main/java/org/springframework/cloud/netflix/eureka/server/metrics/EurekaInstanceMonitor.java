/*
 * Copyright 2013-2024 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.server.metrics;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;

import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceCanceledEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRenewedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;

/**
 * {@link SmartApplicationListener} for collecting event metrics from
 * {@link PeerAwareInstanceRegistry}.
 *
 * @author Wonchul Heo
 * @since 4.1.1
 */
public class EurekaInstanceMonitor implements SmartApplicationListener {

	private final MultiGauge eurekaInstanceStatuses;

	private final PeerAwareInstanceRegistry instanceRegistry;

	private final EurekaInstanceTagsProvider tagProvider;

	EurekaInstanceMonitor(MeterRegistry meterRegistry, PeerAwareInstanceRegistry instanceRegistry,
			EurekaInstanceTagsProvider tagProvider) {
		Objects.requireNonNull(meterRegistry);
		this.instanceRegistry = Objects.requireNonNull(instanceRegistry);
		this.tagProvider = Objects.requireNonNull(tagProvider);
		this.eurekaInstanceStatuses = MultiGauge.builder("eureka.server.instance.status")
				.description("Status of application instances registered with the Eureka server.")
				.register(meterRegistry);
	}

	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return EurekaInstanceCanceledEvent.class.isAssignableFrom(eventType)
				|| EurekaInstanceRegisteredEvent.class.isAssignableFrom(eventType)
				|| EurekaInstanceRenewedEvent.class.isAssignableFrom(eventType);
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		final Map<Tags, Long> aggregatedCounts = instanceRegistry.getApplications().getRegisteredApplications().stream()
				.flatMap(application -> application.getInstances().stream())
				.collect(Collectors.groupingBy(this::tags, Collectors.counting()));
		eurekaInstanceStatuses.register(aggregatedCounts.entrySet().stream()
				.map(it -> MultiGauge.Row.of(it.getKey(), it.getValue())).collect(Collectors.toList()), true);
	}

	private Tags tags(InstanceInfo instanceInfo) {
		return tagProvider.eurekaInstanceTags(instanceInfo).and("status", instanceInfo.getStatus().name());
	}

}
