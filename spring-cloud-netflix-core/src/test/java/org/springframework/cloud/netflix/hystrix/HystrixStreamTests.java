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

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.net.URL;

import static org.junit.Assert.*;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = HystrixStreamTests.Application.class)
@WebAppConfiguration
@IntegrationTest({ "server.port=0", "spring.application.name=hystrixstreamtest" })
@DirtiesContext
public class HystrixStreamTests {

	@Value("${local.server.port}")
	private int port = 0;

	@Test
	public void hystrixStreamWorks() throws Exception {
		String url = "http://localhost:" + port;
		//you have to hit a Hystrix circuit breaker before the stream sends anything
		ResponseEntity<String> response = new TestRestTemplate().getForEntity(url, String.class);
		assertEquals("bad response code", HttpStatus.OK, response.getStatusCode());

		URL hystrixUrl = new URL(url + "/hystrix.stream");
		InputStream in = hystrixUrl.openStream();
		byte[] buffer = new byte[1024];
		in.read(buffer);
		String contents = new String(buffer);
		assertTrue(contents.contains("ping"));
		in.close();
	}

	@Configuration
	@EnableAutoConfiguration
	@RestController
	@EnableCircuitBreaker
	protected static class Application {
		@Autowired
		Service service;

		@Bean
		Service service() {
			return new Service();
		}

		@RequestMapping("/")
		public String hello() {
			return service.hello();
		}
	}

	protected static class Service {
		@HystrixCommand
		public String hello() {
			return "Hello World";
		}
	}
}
