/*
 * Copyright 2013-2016 the original author or authors.
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

package org.springframework.cloud.netflix.ribbon.eureka;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = RibbonEurekaAutoConfigurationTests.EurekaClientDisabledApp.class,
		properties = { "eureka.client.enabled=false", "spring.application.name=eurekadisabledtest" },
		webEnvironment = RANDOM_PORT)
@DirtiesContext
public class RibbonEurekaAutoConfigurationTests {

	@Autowired
	TestLoadbalancerClient testLoadbalancerClient;

	@Test
	public void contextLoads() {
		assertThat(testLoadbalancerClient.instanceFound).isFalse();
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	public static class EurekaClientDisabledApp {

		@Bean
		public TestLoadbalancerClient testLoadbalanceClient(LoadBalancerClient loadBalancerClient) {
			return new TestLoadbalancerClient(loadBalancerClient);
		}

		@Bean
		public CommandLineRunner commandLineRunner(final TestLoadbalancerClient testLoadbalancerClient) {
			return new CommandLineRunner() {
				@Override
				public void run(String... args) throws Exception {
					testLoadbalancerClient.doStuff();
				}
			};
		}
	}

	private static class TestLoadbalancerClient {

		Log log = LogFactory.getLog(this.getClass());

		private LoadBalancerClient loadBalancerClient;
		private boolean instanceFound = false;

		public TestLoadbalancerClient(LoadBalancerClient loadBalancerClient) {
			this.loadBalancerClient = loadBalancerClient;
		}

		public void doStuff() {
			ServiceInstance serviceInstance = loadBalancerClient.choose("http://host/doStuff");
			if (serviceInstance != null) {
				log.info("There is a service instance, because Eureka discovery is enabled and the service is registered");
				instanceFound = true;
			}
			else {
				log.warn("No instance found, because Eureka is disabled or there is no service matching.");
			}
		}
	}

}
