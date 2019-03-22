/*
 * Copyright 2013-2019 the original author or authors.
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

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.netflix.hystrix.dashboard.HystrixDashboardContextTests.Application;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = WebEnvironment.RANDOM_PORT, properties = {
		"spring.application.name=hystrix-dashboard",
		"server.servlet.context-path=/context" })
public class HystrixDashboardContextTests {

	public static final String JQUERY_PATH = "/context/webjars/jquery/2.1.1/jquery.min.js";

	@LocalServerPort
	private int port = 0;

	@Test
	public void homePage() {
		ResponseEntity<String> entity = new TestRestTemplate().getForEntity(
				"http://localhost:" + this.port + "/context/hystrix", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		String body = entity.getBody();
		assertThat(body.contains("base href=\"/context/hystrix\""))
				.as("wrong base path rendered in template").isTrue();
	}

	@Test
	public void correctJavascriptLink() {
		ResponseEntity<String> entity = new TestRestTemplate().getForEntity(
				"http://localhost:" + this.port + "/context/hystrix", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		String body = entity.getBody();
		assertThat(body.contains("src=\"" + JQUERY_PATH + "\""))
				.as("wrong jquery path rendered in template").isTrue();
	}

	@Test
	public void cssAvailable() {
		ResponseEntity<String> entity = new TestRestTemplate().getForEntity(
				"http://localhost:" + this.port + "/context/hystrix/css/global.css",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	public void webjarsAvailable() {
		ResponseEntity<String> entity = new TestRestTemplate().getForEntity(
				"http://localhost:" + this.port + JQUERY_PATH, String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	public void monitorPage() {
		ResponseEntity<String> entity = new TestRestTemplate().getForEntity(
				"http://localhost:" + this.port + "/context/hystrix/monitor",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		String body = entity.getBody();
		assertThat(body.contains("base href=\"/context/hystrix/monitor\""))
				.as("wrong base path rendered in template").isTrue();
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableHystrixDashboard
	protected static class Application {

	}

}
