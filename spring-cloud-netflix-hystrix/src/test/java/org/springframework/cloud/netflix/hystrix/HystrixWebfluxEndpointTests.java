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

package org.springframework.cloud.netflix.hystrix;

import java.util.Map;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.test.TestAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Dave Syer
 * @author Spencer Gibb
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT,
		properties = { "spring.main.web-application-type=reactive",
				"spring.application.name=hystrixstreamwebfluxtest" /* "debug=true" */ })
@DirtiesContext
public class HystrixWebfluxEndpointTests {

	private static final String BASE_PATH = new WebEndpointProperties().getBasePath();

	private static final Log log = LogFactory.getLog(HystrixWebfluxEndpointTests.class);

	@LocalServerPort
	private int port;

	@Test
	public void hystrixStreamWorks() {
		String url = "http://localhost:" + port;
		// you have to hit a Hystrix circuit breaker before the stream sends anything
		WebTestClient testClient = WebTestClient.bindToServer().baseUrl(url).build();
		testClient.get().uri("/").exchange().expectStatus().isOk();

		WebClient client = WebClient.create(url);

		Flux<String> result = client.get().uri(BASE_PATH + "/hystrix.stream")
				.accept(MediaType.TEXT_EVENT_STREAM).exchange()
				.flatMapMany(res -> res.bodyToFlux(Map.class)).take(5)
				.filter(map -> "HystrixCommand".equals(map.get("type")))
				.map(map -> (String) map.get("type"));

		StepVerifier.create(result).expectNext("HystrixCommand").thenCancel().verify();
	}

	@RestController
	@EnableCircuitBreaker
	@EnableAutoConfiguration(exclude = TestAutoConfiguration.class, excludeName = {
			"org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration",
			"org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration",
			"org.springframework.boot.actuate.autoconfigure.security.reactive.ReactiveManagementWebSecurityAutoConfiguration" })
	@SpringBootConfiguration
	protected static class Config {

		@HystrixCommand
		@RequestMapping("/")
		public String hi() {
			return "hi";
		}

	}

}
