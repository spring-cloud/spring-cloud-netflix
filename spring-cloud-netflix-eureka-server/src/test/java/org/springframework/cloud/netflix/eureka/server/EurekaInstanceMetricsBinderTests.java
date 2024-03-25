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

package org.springframework.cloud.netflix.eureka.server;

import java.util.List;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.netflix.eureka.server.metrics.EurekaInstanceMetricsBinder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.netflix.eureka.server.InstanceRegistryTests.getInstanceInfo;
import static org.springframework.cloud.netflix.eureka.server.InstanceRegistryTests.getLeaseInfo;

@SpringBootTest(classes = InstanceRegistryTests.TestApplication.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, value = { "spring.application.name=eureka",
				"logging.level.org.springframework.cloud.netflix.eureka.server.InstanceRegistry=DEBUG" })
class EurekaInstanceMetricsBinderTests {

	private static final String APP_NAME = "MY-APP-NAME";

	private static final String HOST_NAME = "my-host-name";

	private static final String INSTANCE_ID = "my-host-name:8008";

	private static final int PORT = 8008;

	@SpyBean(PeerAwareInstanceRegistry.class)
	private InstanceRegistry instanceRegistry;

	@Autowired
	private MeterRegistry meterRegistry;

	@Autowired
	private EurekaInstanceMetricsBinder binder;

	@Test
	void testMetrics() {
		final InstanceInfo instanceInfo = getInstanceInfo(APP_NAME, HOST_NAME, INSTANCE_ID, PORT, getLeaseInfo());
		instanceRegistry.register(instanceInfo, false);

		binder.bindTo(meterRegistry);

		assertThat(meterRegistry.get("eureka.server.application.instances")
				.tags(List.of(Tag.of("application", APP_NAME), Tag.of("id", INSTANCE_ID), Tag.of("host", HOST_NAME),
						Tag.of("port", String.valueOf(PORT)), Tag.of("status", InstanceInfo.InstanceStatus.UP.name()))))
								.isNotNull();
	}

}
