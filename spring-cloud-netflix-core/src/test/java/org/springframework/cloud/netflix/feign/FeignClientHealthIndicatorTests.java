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

package org.springframework.cloud.netflix.feign;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.SocketUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.Assert.*;

/**
 * @author Eko Kurniawan Khannedy
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = FeignClientHealthIndicatorTests.Application.class, webEnvironment = WebEnvironment.DEFINED_PORT)
@DirtiesContext
public class FeignClientHealthIndicatorTests {

	@Autowired
	private ApplicationContext applicationContext;

	@BeforeClass
	public static void beforeClass() {
		int port = SocketUtils.findAvailableTcpPort();
		System.setProperty("server.port", String.valueOf(port));
	}

	@AfterClass
	public static void afterClass() {
		System.clearProperty("server.port");
	}

	@Test
	public void testHealthy() {
		HealthIndicator indicator = applicationContext.getBean("healthClientHealthIndicator", HealthIndicator.class);
		Health health = indicator.health();

		assertEquals(Status.UP, health.getStatus());
		assertEquals(HttpStatus.OK.value(), health.getDetails().get("statusCode"));
		assertEquals("OK", health.getDetails().get("responseBody"));
	}

	@Test
	public void testBasic() {
		HealthIndicator indicator = applicationContext.getBean("basicClientHealthIndicator", HealthIndicator.class);
		Health health = indicator.health();

		assertEquals(Status.UP, health.getStatus());
		assertEquals("OK", health.getDetails().get("responseBody"));
	}

	@Test
	public void testDown() {
		HealthIndicator indicator = applicationContext.getBean("downClientHealthIndicator", HealthIndicator.class);
		Health health = indicator.health();
		assertEquals(Status.DOWN, health.getStatus());
	}

	@FeignClient(name = "healthClient", url = "http://localhost:${server.port}", health = "health")
	protected interface HealthClient {

		@RequestMapping(method = RequestMethod.GET, value = "/health")
		ResponseEntity<String> health();
	}

	@FeignClient(name = "basicClient", url = "http://localhost:${server.port}", health = "basic")
	protected interface BasicClient {

		@RequestMapping(method = RequestMethod.GET, value = "/basic")
		String basic();
	}

	@FeignClient(name = "downClient", url = "http://localhost:${server.port}", health = "down")
	protected interface DownClient {

		@RequestMapping(method = RequestMethod.GET, value = "/down")
		ResponseEntity<String> down();
	}

	@Configuration
	@EnableAutoConfiguration
	@Import({FeignAutoConfiguration.class})
	@EnableFeignClients(clients = {HealthClient.class, DownClient.class, BasicClient.class})
	@RestController
	protected static class Application implements HealthClient, DownClient, BasicClient {

		@Override
		public ResponseEntity<String> health() {
			return ResponseEntity.ok("OK");
		}

		@Override
		public ResponseEntity<String> down() {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("KO");
		}

		@Override
		public String basic() {
			return "OK";
		}
	}

}
