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

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Dave Syer
 * @author Spencer Gibb
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = HystrixStreamEndpointTests.Application.class,
		webEnvironment = WebEnvironment.RANDOM_PORT, value = {
		"spring.application.name=hystrixstreamtest" })
@DirtiesContext
public class HystrixStreamEndpointTests {

	private static final Log log = LogFactory.getLog(HystrixStreamEndpointTests.class);

	@LocalServerPort
	private int port = 0;

	@Test
	public void pathStartsWithSlash() {
		HystrixStreamEndpoint endpoint = new HystrixStreamEndpoint();
		assertEquals("/hystrix.stream", endpoint.getPath());
	}

	@Test
	public void hystrixStreamWorks() throws Exception {
		String url = "http://localhost:" + port;
		// you have to hit a Hystrix circuit breaker before the stream sends anything
		ResponseEntity<String> response = new TestRestTemplate().getForEntity(url,
				String.class);
		assertEquals("bad response code", HttpStatus.OK, response.getStatusCode());

		URL hystrixUrl = new URL(url + "/admin/hystrix.stream");

		List<String> data = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			try (InputStream in = hystrixUrl.openStream()) {
				byte[] buffer = new byte[1024];
				in.read(buffer);
				data.add(new String(buffer));
			} catch (Exception e) {
				log.error("Error getting hystrix stream, try " + i, e);
			}
		}

		for (String item : data) {
			if (item.contains("data:")) {
				return; // test passed
			}
		}
		fail("/hystrix.stream didn't contain 'data:' was " + data);
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
