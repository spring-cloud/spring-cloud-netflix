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

package org.springframework.cloud.netflix.eureka.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.netflix.eureka.http.WebClientEurekaHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Spencer Gibb
 */
@SpringBootTest(properties = { "spring.cloud.config.discovery.enabled=true", "eureka.client.enabled=true",
		"spring.config.use-legacy-processing=true", "eureka.client.webclient.enabled=true",
		"spring.codec.max-in-memory-size=310000" }, webEnvironment = RANDOM_PORT)
class EurekaConfigServerBootstrapConfigurationWebClientIntegrationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private WebClientEurekaHttpClient eurekaHttpClient;

	@Test
	void webClientRespectsCodecProperties() {
		WebClient webClient = eurekaHttpClient.getWebClient();
		ClientResponse response = webClient.get().uri("http://localhost:" + port).exchange().block();
		assertThat(response).isNotNull();
		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.bodyToMono(String.class).block()).startsWith("....").hasSize(300000);
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@RestController
	static class WebClientController {

		@GetMapping("/")
		public String hello() {
			return ".".repeat(300000);
		}

		@Bean
		public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
			http.authorizeHttpRequests().anyRequest().permitAll().and().csrf().disable();
			return http.build();
		}

	}

}
