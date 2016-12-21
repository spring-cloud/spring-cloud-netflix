/*
 *
 *  * Copyright 2013-2016 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package org.springframework.cloud.netflix.feign.valid.scanning;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.testclients.TestClient;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Ryan Baxter
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = FeignClientEnvVarTests.Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, value = {
		"spring.application.name=feignclienttest", "feign.httpclient.enabled=false",
		"basepackage=org.springframework.cloud.netflix.feign.testclients" })
@DirtiesContext
public class FeignClientEnvVarTests {

	@Autowired
	private TestClient testClient;

	@Test
	public void testSimpleType() {
		String hello = this.testClient.getHello();
		assertNotNull("hello was null", hello);
		assertEquals("first hello didn't match", "hello world 1", hello);
	}

	@Configuration
	@EnableAutoConfiguration
	@RestController
	@EnableFeignClients(basePackages = {"${basepackage}"})
	@RibbonClient(name = "localapp", configuration = LocalRibbonClientConfiguration.class)
	protected static class Application {
		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		public String getHello() {
			return "hello world 1";
		}

		public static void main(String[] args) {
			new SpringApplicationBuilder(Application.class)
					.properties("spring.application.name=feignclienttest",
							"management.contextPath=/admin")
					.run(args);
		}
	}

	// Load balancer with fixed server list for "local" pointing to localhost
	@Configuration
	public static class LocalRibbonClientConfiguration {

		@Value("${local.server.port}")
		private int port = 0;

		@Bean
		public ServerList<Server> ribbonServerList() {
			return new StaticServerList<>(new Server("localhost", this.port));
		}

	}
}
