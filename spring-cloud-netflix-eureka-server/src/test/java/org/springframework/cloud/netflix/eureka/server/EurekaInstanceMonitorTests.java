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

import java.util.Map;
import java.util.stream.Collectors;

import com.netflix.appinfo.InstanceInfo;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.cloud.netflix.eureka.server.EurekaInstancesFixture.getInstanceInfo;
import static org.springframework.cloud.netflix.eureka.server.EurekaInstancesFixture.getLeaseInfo;

/**
 * @author Wonchul Heo
 */
@SpringBootTest(classes = EurekaInstanceMonitorTests.Application.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		value = { "spring.application.name=eureka", "eureka.server.metrics.enabled=true" })
class EurekaInstanceMonitorTests {

	private static final String APP_NAME = "MY-APP-NAME";

	@Autowired
	private InstanceRegistry instanceRegistry;

	@Autowired
	private MeterRegistry meterRegistry;

	private InstanceInfo instanceInfo;

	private InstanceInfo instanceInfo2;

	@BeforeEach
	void setup() {
		instanceRegistry.clearRegistry();
		meterRegistry.clear();
		instanceInfo = getInstanceInfo(APP_NAME, "my-host-name", "my-host-name:8008", 8008, getLeaseInfo());
		instanceInfo2 = getInstanceInfo(APP_NAME, "my-host-name", "my-host-name:8009", 8009, getLeaseInfo());
	}

	@Test
	void testNoRegistration() {
		assertThatThrownBy(() -> {
			meterRegistry.get("eureka.server.instance.status").meter();
		}).isInstanceOf(MeterNotFoundException.class);
	}

	@Test
	void testMultipleRegistrations() {
		instanceRegistry.register(instanceInfo, false);
		instanceRegistry.register(instanceInfo2, false);

		final Map<Tags, Long> counts = Map.of(tags(instanceInfo), 2L);
		assertEurekaInstance(counts, counts);
	}

	@Test
	void testPartialDeregistrationAfterMultipleRegistrations() {
		instanceRegistry.register(instanceInfo, false);
		instanceRegistry.register(instanceInfo2, false);
		instanceRegistry.internalCancel(instanceInfo.getAppName(), instanceInfo.getInstanceId(), false);

		final Map<Tags, Long> counts = Map.of(tags(instanceInfo), 1L);
		assertEurekaInstance(counts, counts);
	}

	@Test
	void testPartialDeregistrationAndThenRegistrationAfterMultipleRegistrations() {
		instanceRegistry.register(instanceInfo, false);
		instanceRegistry.register(instanceInfo2, false);
		instanceRegistry.internalCancel(instanceInfo.getAppName(), instanceInfo.getInstanceId(), false);
		instanceRegistry.register(instanceInfo, false);

		final Map<Tags, Long> counts = Map.of(tags(instanceInfo), 2L);
		assertEurekaInstance(counts, counts);
	}

	@Test
	void testPartialNonRenewalAfterMultipleRegistrations() {
		instanceRegistry.register(instanceInfo, false);
		instanceRegistry.register(instanceInfo2, false);

		instanceRegistry.statusUpdate(instanceInfo.getAppName(), instanceInfo.getInstanceId(),
				InstanceInfo.InstanceStatus.DOWN, null, false);

		final Map<Tags, Long> instanceRegistryCounts = Map.of(tags(instanceInfo), 1L, tags(instanceInfo2), 1L);
		final Map<Tags, Long> meterRegistryCounts = Map.of(tags(instanceInfo2), 2L);
		assertEurekaInstance(instanceRegistryCounts, meterRegistryCounts);
	}

	@Test
	void testPartialRenewalAfterMultipleRegistrations() {
		instanceRegistry.register(instanceInfo, false);
		instanceRegistry.register(instanceInfo2, false);

		instanceRegistry.statusUpdate(instanceInfo.getAppName(), instanceInfo.getInstanceId(),
				InstanceInfo.InstanceStatus.DOWN, null, false);
		instanceRegistry.renew(instanceInfo.getAppName(), instanceInfo.getInstanceId(), false);

		final Map<Tags, Long> counts = Map.of(tags(instanceInfo), 1L, tags(instanceInfo2), 1L);
		assertEurekaInstance(counts, counts);
	}

	private static Tags tags(InstanceInfo instanceInfo) {
		return Tags.of(Tag.of("application", instanceInfo.getAppName()),
				Tag.of("status", instanceInfo.getStatus().name()));
	}

	private void assertEurekaInstance(Map<Tags, Long> instanceRegistryCounts, Map<Tags, Long> meterRegistryCounts) {
		final Map<Tags, Long> actualInstanceRegistryCounts = instanceRegistry.getApplications()
				.getRegisteredApplications().stream().flatMap(application -> application.getInstances().stream())
				.collect(Collectors.groupingBy(EurekaInstanceMonitorTests::tags, Collectors.counting()));
		assertThat(actualInstanceRegistryCounts).isEqualTo(instanceRegistryCounts);

		meterRegistryCounts.forEach((tags, count) -> {
			assertThat((long) meterRegistry.get("eureka.server.instance.status").tags(tags).gauge().value())
					.isEqualTo(count);
		});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@EnableEurekaServer
	protected static class Application {

	}
}
