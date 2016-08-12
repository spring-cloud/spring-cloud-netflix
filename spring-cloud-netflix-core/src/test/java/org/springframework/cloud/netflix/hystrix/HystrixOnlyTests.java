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

package org.springframework.cloud.netflix.hystrix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Base64;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = HystrixOnlyApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class HystrixOnlyTests {

	@Value("${local.server.port}")
	private int port;
	
	@Value("${security.user.username}")
	private String username;

	@Value("${security.user.password}")
	private String password;

	@Test
	public void testNormalExecution() {
		String s = new TestRestTemplate()
				.getForObject("http://localhost:" + this.port + "/", String.class);
		assertEquals("incorrect response", "Hello world", s);
	}

	@Test
	public void testFailureFallback() {
		String s = new TestRestTemplate()
				.getForObject("http://localhost:" + this.port + "/fail", String.class);
		assertEquals("incorrect fallback", "Fallback Hello world", s);
	}

	@Test
	public void testHystrixHealth() {
		Map<?, ?> map = getHealth();
		assertTrue("Missing hystrix health key", map.containsKey("hystrix"));
		Map<?, ?> hystrix = (Map<?, ?>) map.get("hystrix");
		assertEquals("Wrong hystrix status", "UP", hystrix.get("status"));
	}

	@Test
	public void testNoDiscoveryHealth() {
		Map<?, ?> map = getHealth();
		// There is explicitly no discovery, so there should be no discovery health key
		assertFalse("Incorrect existing discovery health key",
				map.containsKey("discovery"));
	}



	private Map<?, ?> getHealth() {
		return new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/admin/health", HttpMethod.GET,
				new HttpEntity<Void>(createBasicAuthHeader(username, password)),
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

	public static void main(String[] args) {
		SpringApplication.run(HystrixOnlyApplication.class, args);
	}

}
