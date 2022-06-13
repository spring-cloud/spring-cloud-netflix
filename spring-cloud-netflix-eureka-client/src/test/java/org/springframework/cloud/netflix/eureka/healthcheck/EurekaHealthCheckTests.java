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

package org.springframework.cloud.netflix.eureka.healthcheck;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the Eureka health check handler.
 *
 * @author Jakub Narloch
 * @author Olga Maciaszek-Sharma
 */
@SpringBootTest(classes = EurekaHealthCheckTests.EurekaHealthCheckApplication.class,
		webEnvironment = WebEnvironment.RANDOM_PORT, value = { "eureka.client.healthcheck.enabled=true", "debug=true" })
@DirtiesContext
class EurekaHealthCheckTests {

	@Autowired
	private EurekaClient discoveryClient;

	@Test
	void shouldRegisterService() {
		System.setProperty("status", "UP");

		InstanceInfo.InstanceStatus status = this.discoveryClient.getHealthCheckHandler()
				.getStatus(InstanceInfo.InstanceStatus.UNKNOWN);

		assertThat(status).isNotNull();
		assertThat(status).isEqualTo(InstanceInfo.InstanceStatus.UP);
	}

	@Test
	void shouldMapOutOfServiceToDown() {
		System.setProperty("status", "OUT_OF_SERVICE");

		InstanceInfo.InstanceStatus status = this.discoveryClient.getHealthCheckHandler()
				.getStatus(InstanceInfo.InstanceStatus.UNKNOWN);

		assertThat(status).isNotNull();
		assertThat(status).isEqualTo(InstanceInfo.InstanceStatus.DOWN);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	protected static class EurekaHealthCheckApplication {

		@Bean
		public HealthIndicator healthIndicator() {
			return () -> new Health.Builder().status(System.getProperty("status")).build();
		}

	}

}
