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

import java.util.List;
import java.util.Objects;

import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * @author heowc
 */
public class EurekaInstanceMetricsBinder implements MeterBinder {

	private final PeerAwareInstanceRegistry instanceRegistry;

	EurekaInstanceMetricsBinder(PeerAwareInstanceRegistry instanceRegistry) {
		this.instanceRegistry = Objects.requireNonNull(instanceRegistry);
	}

	@Override
	public void bindTo(MeterRegistry meterRegistry) {
		instanceRegistry.getApplications().getRegisteredApplications().stream()
				.flatMap(application -> application.getInstances().stream()).forEach(instanceInfo -> {
					Gauge.builder("eureka.server.application.instances", () -> 1L)
							.description("Information about application instances registered on the Eureka server.")
							.tags(List.of(Tag.of("application", instanceInfo.getAppName()),
									Tag.of("id", instanceInfo.getId()), Tag.of("host", instanceInfo.getHostName()),
									Tag.of("port", String.valueOf(instanceInfo.getPort())),
									Tag.of("status", instanceInfo.getStatus().name())))
							.register(meterRegistry);
				});
	}

}
