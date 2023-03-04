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

package org.springframework.cloud.netflix.eureka.server;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.netflix.eureka.server.ApplicationContextTests.Application;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Application.class, webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "spring.application.name=eureka", "server.servlet.context-path=/context",
				"management.security.enabled=false", "management.endpoints.web.exposure.include=*",
				"eureka.server.version.filter.debug.response-header=X-Version-Filter-Computed-Path" })
class ApplicationContextTests {

	private static final String BASE_PATH = new WebEndpointProperties().getBasePath();

	@LocalServerPort
	private int port = 0;

	@Test
	void catalogLoads() {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = new TestRestTemplate()
				.getForEntity("http://localhost:" + this.port + "/context/eureka/apps", Map.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		String computedPath = entity.getHeaders().getFirst("X-Version-Filter-Computed-Path");
		assertThat(computedPath).isEqualTo("/context/eureka/v2/apps");
	}

	@Test
	void dashboardLoads() {
		ResponseEntity<String> entity = new TestRestTemplate()
				.getForEntity("http://localhost:" + this.port + "/context/", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		String body = entity.getBody();
		// System.err.println(body);
		assertThat(body.contains("eureka/js")).isTrue();
		assertThat(body.contains("eureka/css")).isTrue();
		// The "DS Replicas"
		assertThat(body.contains("<a href=\"http://localhost:8761/eureka/\">localhost</a>")).isTrue();
	}

	@Test
	void cssAvailable() {
		ResponseEntity<String> entity = new TestRestTemplate()
				.getForEntity("http://localhost:" + this.port + "/context/eureka/css/wro.css", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void jsAvailable() {
		ResponseEntity<String> entity = new TestRestTemplate()
				.getForEntity("http://localhost:" + this.port + "/context/eureka/js/wro.js", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void adminLoads() {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/context" + BASE_PATH + "/env", HttpMethod.GET,
				new HttpEntity<>("parameters", headers), Map.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@EnableEurekaServer
	protected static class Application {

	}

}
