/*
 * Copyright 2013-2018 the original author or authors.
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
 *
 */

package org.springframework.cloud.netflix.hystrix;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.test.NoSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.cloud.netflix.test.TestAutoConfiguration.PASSWORD;
import static org.springframework.cloud.netflix.test.TestAutoConfiguration.USER;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = HystrixOnlyApplication.class, webEnvironment = RANDOM_PORT,
		properties = {"management.endpoint.health.show-details=ALWAYS"})
@DirtiesContext
@ActiveProfiles("proxysecurity")
public class HystrixOnlyTests {
	private static final String BASE_PATH = new WebEndpointProperties().getBasePath();

	@LocalServerPort
	private int port;

	@Test
	public void testNormalExecution() {
		ResponseEntity<String> res = new TestRestTemplate()
				.getForEntity("http://localhost:" + this.port + "/", String.class);
		assertEquals("incorrect response", "Hello world", res.getBody());
	}

	@Test
	public void testFailureFallback() {
		ResponseEntity<String> res = new TestRestTemplate()
				.getForEntity("http://localhost:" + this.port + "/fail", String.class);
		assertEquals("incorrect fallback", "Fallback Hello world", res.getBody());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testHystrixHealth() {
		Map map = getHealth();
		assertThat(map).containsKeys("details");
		Map details = (Map) map.get("details");
		assertThat(details).containsKeys("hystrix");
		Map hystrix = (Map) details.get("hystrix");
		assertThat(hystrix).containsEntry("status", "UP");
	}

	@Test
	public void testNoDiscoveryHealth() {
		Map<?, ?> map = getHealth();
		// There is explicitly no discovery, so there should be no discovery health key
		assertFalse("Incorrect existing discovery health key",
				map.containsKey("discovery"));
	}

	@Test
	public void testHystrixInnerMapMetrics() {
		// We have to hit any Hystrix command before Hystrix metrics to be populated
		String url = "http://localhost:" + this.port;
		ResponseEntity<String> response = new TestRestTemplate().getForEntity(url,
				String.class);
		assertEquals("bad response code", HttpStatus.OK, response.getStatusCode());

		// Poller takes some time to realize for new metrics
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {}

		Map<String, List<String>> map = (Map<String, List<String>>) getMetrics();

		assertTrue("There is no latencyTotal group key specified",
				map.get("names").contains("hystrix.latency.total"));
		assertTrue("There is no latencyExecute group key specified",
				map.get("names").contains("hystrix.latency.execution"));
	}


	private Map<?, ?> getMetrics() {
		return getAuthenticatedEndpoint("/metrics");
	}

	private Map<?, ?> getHealth() {
		return getAuthenticatedEndpoint("/health");
	}

	private Map<?, ?> getAuthenticatedEndpoint(String endpoint) {
		return new TestRestTemplate().exchange(
				"http://localhost:" + this.port + BASE_PATH + endpoint, HttpMethod.GET,
				new HttpEntity<Void>(createBasicAuthHeader(USER, PASSWORD)),
				Map.class).getBody();
	}

	public static HttpHeaders createBasicAuthHeader(final String username,
													final String password) {
		return new HttpHeaders() {
			private static final long serialVersionUID = 1766341693637204893L;

			{
				String auth = username + ":" + password;
				byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
				String authHeader = "Basic " + new String(encodedAuth);
				this.set("Authorization", authHeader);
			}
		};
	}
}

class Service {
	@HystrixCommand
	public String hello() {
		return "Hello world";
	}

	@HystrixCommand(fallbackMethod = "fallback")
	public String fail() {
		throw new RuntimeException("Always fail");
	}

	public String fallback() {
		return "Fallback Hello world";
	}
}

// Don't use @SpringBootApplication because we don't want to component scan
@Configuration
@EnableAutoConfiguration
@EnableCircuitBreaker
@RestController
@Import(NoSecurityConfiguration.class)
class HystrixOnlyApplication {

	@Bean
	public Service service() {
		return new Service();
	}

	@Autowired
	private Service service;

	@RequestMapping("/")
	public String home() {
		return this.service.hello();
	}

	@RequestMapping("/fail")
	public String fail() {
		return this.service.fail();
	}

}
