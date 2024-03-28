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

import com.netflix.appinfo.InstanceInfo;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.eureka.server.metrics.EurekaInstanceMeterBinder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.netflix.eureka.server.FixtureEurekaInstances.getInstanceInfo;
import static org.springframework.cloud.netflix.eureka.server.FixtureEurekaInstances.getLeaseInfo;

@SpringBootTest(classes = InstanceRegistryTests.TestApplication.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, value = { "spring.application.name=eureka" })
class EurekaInstanceMeterBinderTests {

	@Autowired
	private InstanceRegistry instanceRegistry;

	@Autowired
	private MeterRegistry meterRegistry;

	@Autowired
	private EurekaInstanceMeterBinder binder;

	@Test
	void testBindTo() {
		final String expectedAppName = "MY-APP-NAME";
		final InstanceInfo instanceInfo = getInstanceInfo(expectedAppName, "my-host-name", "my-host-name:8008", 8008, getLeaseInfo());
		instanceRegistry.register(instanceInfo, false);

		binder.bindTo(meterRegistry);

		assertThat(meterRegistry.get("eureka.server.application.instances")
				.tag("application", expectedAppName)
				.tag("status", InstanceInfo.InstanceStatus.UP.name()))
				.isNotNull();
	}

}
