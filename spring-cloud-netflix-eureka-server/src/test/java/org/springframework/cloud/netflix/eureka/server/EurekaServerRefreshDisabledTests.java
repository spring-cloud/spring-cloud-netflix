/*
 * Copyright 2013-present the original author or authors.
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

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Verifies the behaviour of Eureka server with peer registration enabled and refresh
 * disabled.
 *
 * @author Olga Maciaszek-Sharma
 */
@SpringBootTest(classes = EurekaServerRefreshDisabledTests.Application.class, webEnvironment = RANDOM_PORT,
		properties = { "management.endpoints.web.exposure.include=*", "spring.cloud.refresh.enabled=false" })
public class EurekaServerRefreshDisabledTests {

	@LocalServerPort
	private int port = 0;

	// verifies GH-4407
	@SuppressWarnings("rawtypes")
	@Test
	void appCatalogLoads() {
		ResponseEntity<Map> entity = new TestRestTemplate()
			.getForEntity("http://localhost:" + this.port + "/eureka/apps", Map.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@EnableEurekaServer
	protected static class Application {

	}

}
