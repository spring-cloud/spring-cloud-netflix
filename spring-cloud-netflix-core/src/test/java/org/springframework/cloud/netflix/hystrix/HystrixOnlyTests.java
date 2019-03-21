/*
 * Copyright 2013-2015 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = HystrixOnlyApplication.class)
@WebAppConfiguration
@IntegrationTest({ "server.port: 0" })
@DirtiesContext
public class HystrixOnlyTests {

	@Value("${local.server.port}")
	private int port;

	@Test
	public void testNormalExecution() {
		String s = new TestRestTemplate().getForObject("http://localhost:" + this.port
				+ "/", String.class);
		assertEquals("incorrect response", "Hello world", s);
	}

	@Test
	public void testFailureFallback() {
		String s = new TestRestTemplate().getForObject("http://localhost:" + this.port
				+ "/fail", String.class);
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
		return new TestRestTemplate().getForObject("http://localhost:" + this.port
				+ "/admin/health", Map.class);
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
