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

package org.springframework.cloud.netflix.hystrix.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Spencer Gibb
 * @author Daniel Lavoie
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "debug=true",
		"spring.jmx.enabled=true", "spring.application.name=mytestapp" })
@DirtiesContext
public class HystrixStreamTests {

	@Autowired
	private HystrixStreamTask task;

	@Autowired
	private Application application;

	@Autowired(required = false)
	private Registration registration;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private MessageCollector collector;

	@Autowired
	@Qualifier(HystrixStreamClient.OUTPUT)
	private MessageChannel output;

	@Test
	public void contextLoads() throws Exception {
		this.application.hello();
		// It is important that local service instance resolves for metrics
		// origin details to be populated
		assertThat(this.registration).isNotNull();
		assertThat(this.registration.getServiceId()).isEqualTo("mytestapp");
		this.task.gatherMetrics();
		Message<?> message = this.collector.forChannel(output).take();
		JsonNode tree = mapper.readTree((String) message.getPayload());
		assertThat(tree.hasNonNull("origin")).isTrue();
		assertThat(tree.hasNonNull("data")).isTrue();
		assertThat(tree.hasNonNull("event")).isTrue();
		assertThat(tree.findValue("event").asText()).isEqualTo("message");
	}

	@EnableAutoConfiguration
	@EnableCircuitBreaker
	@RestController
	@SpringBootConfiguration
	public static class Application {

		@HystrixCommand
		@RequestMapping("/")
		public String hello() {
			return "Hello World";
		}

	}

}
