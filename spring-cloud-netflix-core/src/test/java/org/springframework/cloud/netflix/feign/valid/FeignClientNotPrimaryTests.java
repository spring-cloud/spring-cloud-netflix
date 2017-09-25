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
 *
 */

package org.springframework.cloud.netflix.feign.valid;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;

import feign.Logger;

import java.util.List;

/**
 * @author Spencer Gibb
 * @author Jakub Narloch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = FeignClientNotPrimaryTests.Application.class, webEnvironment = WebEnvironment.RANDOM_PORT, value = {
		"spring.application.name=feignclientnotprimarytest",
		"logging.level.org.springframework.cloud.netflix.feign.valid=DEBUG",
		"feign.httpclient.enabled=false", "feign.okhttp.enabled=false" })
@DirtiesContext
public class FeignClientNotPrimaryTests {

	public static final String HELLO_WORLD_1 = "hello world 1";
	public static final String OI_TERRA_2 = "oi terra 2";
	public static final String MYHEADER1 = "myheader1";
	public static final String MYHEADER2 = "myheader2";

	@Value("${local.server.port}")
	private int port = 0;

	@Autowired
	private TestClient testClient;

	@Autowired
	private List<TestClient> testClients;

	@FeignClient(name = "localapp", primary = false)
	protected interface TestClient {
		@RequestMapping(method = RequestMethod.GET, path = "/hello")
		Hello getHello();

	}

	@Configuration
	@EnableAutoConfiguration
	@RestController
	@EnableFeignClients(clients = { TestClient.class} ,
			defaultConfiguration = TestDefaultFeignConfig.class)
	@RibbonClient(name = "localapp", configuration = LocalRibbonClientConfiguration.class)
	protected static class Application {

		@Bean
		@Primary
		public PrimaryTestClient primaryTestClient() {
			return new PrimaryTestClient();
		}

		@RequestMapping(method = RequestMethod.GET, path = "/hello")
		public Hello getHello() {
			return new Hello(HELLO_WORLD_1);
		}

		public static void main(String[] args) {
			new SpringApplicationBuilder(Application.class)
					.properties("spring.application.name=feignclienttest",
							"management.contextPath=/admin")
					.run(args);
		}
	}

	@Test
	public void testClientType() {
		assertThat(this.testClient).as("testClient was of wrong type").isInstanceOf(PrimaryTestClient.class);
	}

	@Test
	public void testClientCount() {
		assertThat(this.testClients).as("testClients was wrong").hasSize(2);
	}

	@Test
	public void testSimpleType() {
		Hello hello = this.testClient.getHello();
		assertNull("hello was null", hello);
	}

	protected static class PrimaryTestClient implements TestClient {
		@Override
		public Hello getHello() {
			return null;
		}
	}

	public static class Hello {
		private String message;

		public Hello() {}

		public Hello(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}

	@Configuration
	public static class TestDefaultFeignConfig {
		@Bean
		Logger.Level feignLoggerLevel() {
			return Logger.Level.FULL;
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
