/*
 * Copyright 2013-2025 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.test.client.TestRestTemplate;
import org.springframework.cloud.netflix.eureka.server.ApplicationContextTests.Application;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Application.class, webEnvironment = WebEnvironment.RANDOM_PORT,
		value = { "spring.application.name=eureka", "eureka.dashboard.path=/dashboard" })
class ApplicationDashboardPathTests {

	@Value("${local.server.port}")
	private int port = 0;

	@Test
	void catalogLoads() {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = new TestRestTemplate()
			.getForEntity("http://localhost:" + this.port + "/eureka/apps", Map.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void dashboardLoads() {
		ResponseEntity<String> entity = new TestRestTemplate()
			.getForEntity("http://localhost:" + this.port + "/dashboard", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		String body = entity.getBody();
		// System.err.println(body);
		assertThat(body.contains("eureka/js")).isTrue();
		assertThat(body.contains("eureka/css")).isTrue();
		// The "DS Replicas"
		assertThat(body.contains("<h1>Instances currently registered with Eureka</h1>")).isTrue();
		// The Home
		assertThat(body.contains("<a class=\"nav-link px-2\" href=\"/dashboard\">Home</a>")).isTrue();
		// The Lastn
		assertThat(body.contains("<a class=\"nav-link px-2\" href=\"/dashboard/lastn\">Last")).isTrue();
	}

	@Test
	void cssAvailable() {
		ResponseEntity<String> entity = new TestRestTemplate()
			.getForEntity("http://localhost:" + this.port + "/eureka/css/wro.css", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void jsAvailable() {
		ResponseEntity<String> entity = new TestRestTemplate()
			.getForEntity("http://localhost:" + this.port + "/eureka/js/wro.js", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

}
