/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.healthcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;

/**
 * Tests the Eureka health check handler.
 *
 * @author Jakub Narloch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = EurekaHealthCheckTests.EurekaHealthCheckApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT, value = {
		"eureka.client.healthcheck.enabled=true", "debug=true" })
@DirtiesContext
public class EurekaHealthCheckTests {

	@Autowired
	private EurekaClient discoveryClient;

	@Test
	public void shouldRegisterService() {

		InstanceInfo.InstanceStatus status = this.discoveryClient.getHealthCheckHandler()
				.getStatus(InstanceInfo.InstanceStatus.UNKNOWN);

		assertNotNull(status);
		assertEquals(InstanceInfo.InstanceStatus.OUT_OF_SERVICE, status);
	}

	@Configuration
	@EnableAutoConfiguration
	protected static class EurekaHealthCheckApplication {

		@Bean
		public HealthIndicator healthIndicator() {
			return new HealthIndicator() {
				@Override
				public Health health() {
					return new Health.Builder().outOfService().build();
				}
			};
		}
	}
}
