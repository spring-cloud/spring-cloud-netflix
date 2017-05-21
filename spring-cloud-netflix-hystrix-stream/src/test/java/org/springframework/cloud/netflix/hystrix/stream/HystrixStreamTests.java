/*
 * Copyright 2013-2017 the original author or authors.
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

package org.springframework.cloud.netflix.hystrix.stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = HystrixStreamTests.Application.class, webEnvironment = WebEnvironment.RANDOM_PORT, value = {
		"server.port=0", "spring.jmx.enabled=true", "spring.application.name=mytestapp" })
@DirtiesContext
public class HystrixStreamTests {

	@Autowired
	private HystrixStreamTask task;

	@Autowired
	private Application application;
	
	@Autowired
	private DiscoveryClient discoveryClient;

	@EnableAutoConfiguration
	@EnableCircuitBreaker
	@RestController
	@Configuration
	public static class Application {

		@HystrixCommand
		@RequestMapping("/")
		public String hello() {
			return "Hello World";
		}
	}

	@Test
	public void contextLoads() {
		this.application.hello();
		//It is important that local service instance resolves for metrics 
		//origin details to be populated
		ServiceInstance localServiceInstance = discoveryClient.getLocalServiceInstance();
		assertThat(localServiceInstance).isNotNull();
		assertThat(localServiceInstance.getServiceId()).isEqualTo("mytestapp");
		this.task.gatherMetrics();
	}

}
