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

import com.netflix.appinfo.InstanceInfo;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.eureka.server.metrics.EurekaInstanceMonitor;
import org.springframework.context.annotation.Configuration;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.springframework.cloud.netflix.eureka.server.EurekaInstanceFixture.getInstanceInfo;
import static org.springframework.cloud.netflix.eureka.server.EurekaInstanceFixture.getLeaseInfo;

/**
 * Tests for {@link EurekaInstanceMonitor}.
 *
 * @author Wonchul Heo
 * @author Olga Maciaszek-Sharma
 */
@SpringBootTest(classes = EurekaInstanceMonitorTests.Application.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		value = { "spring.application.name=eureka", "eureka.server.metrics.enabled=true" })
class EurekaInstanceMonitorTests {

	private static final String FOO_APP_NAME = "FOO-APP-NAME";

	private static final String BAR_APP_NAME = "BAR-APP-NAME";

	@Autowired
	private InstanceRegistry instanceRegistry;

	@Autowired
	private MeterRegistry meterRegistry;

	private InstanceInfo fooInstanceInfo;

	private InstanceInfo fooInstanceInfo2;

	private InstanceInfo barInstanceInfo;

	private InstanceInfo barInstanceInfo2;

	@BeforeEach
	void setup() {
		instanceRegistry.clearRegistry();
		meterRegistry.clear();
		fooInstanceInfo = getInstanceInfo(FOO_APP_NAME, "my-host-name", "my-host-name:8008", 8008, getLeaseInfo());
		fooInstanceInfo2 = getInstanceInfo(FOO_APP_NAME, "my-host-name", "my-host-name:8009", 8009, getLeaseInfo());
		barInstanceInfo = getInstanceInfo(BAR_APP_NAME, "my-host-name", "my-host-name:8010", 8010, getLeaseInfo());
		barInstanceInfo2 = getInstanceInfo(BAR_APP_NAME, "my-host-name", "my-host-name:8011", 8011, getLeaseInfo());
	}

	@Test
	void testNoRegistration() {
		assertThat(meterRegistry.find("eureka.server.instances").gauge()).isNull();
	}

	@Test
	void testMultipleRegistrations() {
		instanceRegistry.register(fooInstanceInfo, false);
		instanceRegistry.register(fooInstanceInfo2, false);
		instanceRegistry.register(barInstanceInfo, false);

		final Map<Tags, Long> counts = Map.of(tags(fooInstanceInfo), 2L, tags(barInstanceInfo), 1L);
		assertEurekaInstance(counts);
	}

	@Test
	void testPartialDeregistrationAfterMultipleRegistrations() {
		instanceRegistry.register(fooInstanceInfo, false);
		instanceRegistry.register(fooInstanceInfo2, false);
		instanceRegistry.register(barInstanceInfo, false);
		instanceRegistry.register(barInstanceInfo2, false);

		instanceRegistry.internalCancel(fooInstanceInfo.getAppName(), fooInstanceInfo.getInstanceId(), false);

		final Map<Tags, Long> counts = Map.of(tags(fooInstanceInfo), 1L, tags(barInstanceInfo), 2L);
		assertEurekaInstance(counts);
	}

	@Test
	void testPartialDeregistrationAndThenRegistrationAfterMultipleRegistrations() {
		instanceRegistry.register(fooInstanceInfo, false);
		instanceRegistry.register(fooInstanceInfo2, false);
		instanceRegistry.register(barInstanceInfo, false);
		instanceRegistry.register(barInstanceInfo2, false);

		instanceRegistry.internalCancel(fooInstanceInfo.getAppName(), fooInstanceInfo.getInstanceId(), false);
		instanceRegistry.register(fooInstanceInfo, false);

		final Map<Tags, Long> counts = Map.of(tags(fooInstanceInfo), 2L, tags(barInstanceInfo), 2L);
		assertEurekaInstance(counts);
	}

	@Test
	void testPartialNonRenewalAfterMultipleRegistrations() {
		instanceRegistry.register(fooInstanceInfo, false);
		instanceRegistry.register(fooInstanceInfo2, false);
		instanceRegistry.register(barInstanceInfo, false);
		instanceRegistry.register(barInstanceInfo2, false);

		instanceRegistry.statusUpdate(fooInstanceInfo.getAppName(), fooInstanceInfo.getInstanceId(),
				InstanceInfo.InstanceStatus.DOWN, null, false);

		final Map<Tags, Long> meterRegistryCounts = Map.of(tags(fooInstanceInfo2), 2L);
		assertEurekaInstance(meterRegistryCounts);
	}

	@Test
	void testPartialRenewalAfterMultipleRegistrations() {
		instanceRegistry.register(fooInstanceInfo, false);
		instanceRegistry.register(fooInstanceInfo2, false);
		instanceRegistry.register(barInstanceInfo, false);
		instanceRegistry.register(barInstanceInfo2, false);

		instanceRegistry.statusUpdate(fooInstanceInfo.getAppName(), fooInstanceInfo.getInstanceId(),
				InstanceInfo.InstanceStatus.DOWN, null, false);
		instanceRegistry.statusUpdate(barInstanceInfo2.getAppName(), barInstanceInfo2.getInstanceId(),
				InstanceInfo.InstanceStatus.STARTING, null, false);
		instanceRegistry.renew(fooInstanceInfo.getAppName(), fooInstanceInfo.getInstanceId(), false);
		instanceRegistry.renew(barInstanceInfo2.getAppName(), barInstanceInfo2.getInstanceId(), false);

		final Map<Tags, Long> counts = Map.of(tags(fooInstanceInfo), 1L, tags(fooInstanceInfo2), 1L,
				tags(barInstanceInfo), 1L, tags(barInstanceInfo2), 1L);
		assertEurekaInstance(counts);
	}

	private static Tags tags(InstanceInfo instanceInfo) {
		return Tags.of(Tag.of("application", instanceInfo.getAppName()),
				Tag.of("status", instanceInfo.getStatus().name()));
	}

	private void assertEurekaInstance(Map<Tags, Long> meterRegistryCounts) {
		await().pollDelay(5, MILLISECONDS)
			.atMost(5, SECONDS)
			.pollInterval(fibonacci())
			.untilAsserted(() -> meterRegistryCounts.forEach((tags,
					count) -> assertThat((long) meterRegistry.get("eureka.server.instances").tags(tags).gauge().value())
						.isEqualTo(count)));
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@EnableEurekaServer
	protected static class Application {

	}

}
