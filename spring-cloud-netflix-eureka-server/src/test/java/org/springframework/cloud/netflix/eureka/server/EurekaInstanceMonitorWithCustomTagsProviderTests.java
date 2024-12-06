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
import io.micrometer.core.instrument.Tags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.eureka.server.metrics.EurekaInstanceMonitor;
import org.springframework.cloud.netflix.eureka.server.metrics.EurekaInstanceTagsProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.springframework.cloud.netflix.eureka.server.EurekaInstanceFixture.getInstanceInfo;
import static org.springframework.cloud.netflix.eureka.server.EurekaInstanceFixture.getLeaseInfo;

/**
 * Tests for {@link EurekaInstanceMonitor} with custom tags provider.
 *
 * @author Wonchul Heo
 * @author Olga Maciaszek-Sharma
 */
@SpringBootTest(classes = EurekaInstanceMonitorWithCustomTagsProviderTests.Application.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		value = { "spring.application.name=eureka", "eureka.server.metrics.enabled=true" })
class EurekaInstanceMonitorWithCustomTagsProviderTests {

	private static final String APP_NAME = "FOO-APP-NAME";

	@Autowired
	private InstanceRegistry instanceRegistry;

	@Autowired
	private MeterRegistry meterRegistry;

	private InstanceInfo firstInstanceInfo;

	private InstanceInfo secondInstanceInfo;

	@BeforeEach
	void setup() {
		instanceRegistry.clearRegistry();
		meterRegistry.clear();
		firstInstanceInfo = getInstanceInfo(APP_NAME, "my-host-name", "my-host-name:8008", 8008, getLeaseInfo());
		secondInstanceInfo = getInstanceInfo(APP_NAME, "my-host-name", "my-host-name:8009", 8009, getLeaseInfo());
	}

	@Test
	void testNoRegistration() {
		assertThat(meterRegistry.find("eureka.server.instances").gauge()).isNull();
	}

	@Test
	void testMultipleRegistrations() {
		instanceRegistry.register(firstInstanceInfo, false);
		instanceRegistry.register(secondInstanceInfo, false);

		final Map<Tags, Long> counts = Map.of(tags(firstInstanceInfo), 1L, tags(secondInstanceInfo), 1L);
		assertEurekaInstance(counts);
	}

	private static Tags tags(InstanceInfo instanceInfo) {
		return Tags.of("port", String.valueOf(instanceInfo.getPort()));
	}

	private void assertEurekaInstance(Map<Tags, Long> meterRegistryCounts) {
		await().pollDelay(5, MILLISECONDS)
			.atMost(6, SECONDS)
			.pollInterval(fibonacci())
			.untilAsserted(() -> meterRegistryCounts.forEach((tags,
					count) -> assertThat((long) meterRegistry.get("eureka.server.instances").tags(tags).gauge().value())
						.isEqualTo(count)));
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@EnableEurekaServer
	protected static class Application {

		@Bean
		EurekaInstanceTagsProvider customEurekaInstanceTagsProvider() {
			return instanceInfo -> Tags.of("port", String.valueOf(instanceInfo.getPort()));
		}

	}

}
