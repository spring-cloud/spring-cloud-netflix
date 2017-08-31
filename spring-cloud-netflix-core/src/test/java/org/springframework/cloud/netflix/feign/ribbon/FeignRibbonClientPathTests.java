/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.netflix.feign.ribbon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;

/**
 * @author Venil Noronha
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = FeignRibbonClientPathTests.Application.class, webEnvironment = WebEnvironment.RANDOM_PORT,value = {
		"spring.application.name=feignribbonclientpathtest",
		"feign.okhttp.enabled=false",
		"feign.httpclient.enabled=false",
		"feign.hystrix.enabled=false",
		"test.path.prefix=/base/path" // For pathWithPlaceholder test
	}
)
@DirtiesContext
public class FeignRibbonClientPathTests {

	@Value("${local.server.port}")
	private int port = 0;

	@Autowired
	private TestClient1 testClient1;

	@Autowired
	private TestClient2 testClient2;

	@Autowired
	private TestClient3 testClient3;

	@Autowired
	private TestClient4 testClient4;
	
	@Autowired
	private TestClient5 testClient5;

	protected interface TestClient {

		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		Hello getHello();

	}

	@FeignClient(name = "localapp", path = "/base/path")
	protected interface TestClient1 extends TestClient { }

	@FeignClient(name = "localapp", path = "base/path")
	protected interface TestClient2 extends TestClient { }

	@FeignClient(name = "localapp", path = "base/path/")
	protected interface TestClient3 extends TestClient { }

	@FeignClient(name = "localapp", path = "/base/path/")
	protected interface TestClient4 extends TestClient { }

	@FeignClient(name = "localapp", path = "${test.path.prefix}")
	protected interface TestClient5 extends TestClient { }
	
	@Configuration
	@EnableAutoConfiguration
	@RestController
	@RequestMapping("/base/path")
	@EnableFeignClients(clients = {
		TestClient1.class, TestClient2.class, TestClient3.class, TestClient4.class,
		TestClient5.class
	})
	@RibbonClient(name = "localapp", configuration = LocalRibbonClientConfiguration.class)
	public static class Application {

		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		public Hello getHello() {
			return new Hello("hello world");
		}

		public static void main(String[] args) throws InterruptedException {
			new SpringApplicationBuilder(Application.class).properties(
				"spring.application.name=feignribbonclientpathtest",
				"management.contextPath=/admin"
			).run(args);
		}

	}

	@Test
	public void pathWithLeadingButNotTrailingSlash() {
		testClientPath(this.testClient1);
	}

	@Test
	public void pathWithoutLeadingAndTrailingSlash() {
		testClientPath(this.testClient2);
	}

	@Test
	public void pathWithoutLeadingButTrailingSlash() {
		testClientPath(this.testClient3);
	}

	@Test
	public void pathWithLeadingAndTrailingSlash() {
		testClientPath(this.testClient4);
	}

	@Test
	public void pathWithPlaceholder() {
		testClientPath(this.testClient5);
	}

	private void testClientPath(TestClient testClient) {
		Hello hello = testClient.getHello();
		assertNotNull("Object returned was null", hello);
		assertEquals("Response object value didn't match", "hello world",
				hello.getMessage());
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
	public static class LocalRibbonClientConfiguration {

		@Value("${local.server.port}")
		private int port = 0;

		@Bean
		public ServerList<Server> ribbonServerList() {
			return new StaticServerList<>(new Server("localhost", this.port));
		}

	}

}
