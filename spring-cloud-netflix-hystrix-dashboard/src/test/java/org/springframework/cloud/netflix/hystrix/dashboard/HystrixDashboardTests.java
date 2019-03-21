/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.cloud.netflix.hystrix.dashboard;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.cloud.netflix.hystrix.dashboard.HystrixDashboardTests.Application;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@IntegrationTest({ "server.port=0", "spring.application.name=hystrix-dashboard" })
public class HystrixDashboardTests {

	@Value("${local.server.port}")
	private int port = 0;

	@Test
	public void homePage() {
		ResponseEntity<String> entity = new TestRestTemplate().getForEntity(
				"http://localhost:" + this.port + "/hystrix", String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		String body = entity.getBody();
		assertTrue(body.contains("<base href=\"/hystrix\">"));
		assertTrue(body.contains("\"/webjars"));
		assertTrue(body.contains("= \"/hystrix/monitor"));
	}

	@Test
	public void cssAvailable() {
		ResponseEntity<String> entity = new TestRestTemplate()
				.getForEntity(
						"http://localhost:" + this.port + "/hystrix/css/global.css",
						String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
	}

	@Test
	public void monitorPage() {
		ResponseEntity<String> entity = new TestRestTemplate().getForEntity(
				"http://localhost:" + this.port + "/hystrix/monitor", String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		String body = entity.getBody();
		assertTrue(body.contains("<base href=\"/hystrix/monitor\">"));
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableHystrixDashboard
	protected static class Application {

		public static void main(String[] args) {
			new SpringApplicationBuilder(Application.class).properties(
					"spring.application.name=hystrix-dashboard").run();
		}

	}

}
