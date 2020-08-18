/*
 * Copyright 2013-2020 the original author or authors.
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
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.netflix.eureka.http.WebClientEurekaHttpClient;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Spencer Gibb
 */
@SpringBootTest(properties = { "spring.cloud.config.discovery.enabled=true",
		"eureka.client.webclient.enabled=true",
		"spring.codec.max-in-memory-size=310000" }, webEnvironment = RANDOM_PORT)
public class EurekaConfigServerBootstrapConfigurationWebClientIntegrationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private WebClientEurekaHttpClient eurekaHttpClient;

	@Test
	public void webClientRespectsCodecProperties() {
		WebClient webClient = eurekaHttpClient.getWebClient();
		ClientResponse response = webClient.get().uri("http://localhost:" + port)
				.exchange().block();
		assertThat(response).isNotNull();
		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.bodyToMono(String.class).block()).startsWith("....")
				.hasSize(300000);
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@RestController
	static class WebClientController extends WebSecurityConfigurerAdapter {

		@GetMapping
		public String hello() {
			StringBuilder s = new StringBuilder();
			for (int i = 0; i < 300000; i++) {
				s.append(".");
			}
			return s.toString();
		}

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http.authorizeRequests().anyRequest().permitAll().and().csrf().disable();
		}

	}

}
