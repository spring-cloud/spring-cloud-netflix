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

package org.springframework.cloud.netflix.feign.valid;

import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import feign.Client;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = FeignHttpClientTests.Application.class)
@WebAppConfiguration
@IntegrationTest({ "server.port=0", "spring.application.name=feignclienttest" })
@DirtiesContext
public class FeignHttpClientTests {

	@Value("${local.server.port}")
	private int port = 0;

	@Autowired
	private TestClient testClient;

	@Autowired
	private Client feignClient;

	@Autowired
	private UserClient userClient;

	// @FeignClient(value = "http://localhost:9876", loadbalance = false)
	@FeignClient("localapp")
	protected static interface TestClient extends BaseTestClient {

	}

	protected static interface BaseTestClient {

		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		public Hello getHello();

		@RequestMapping(method = RequestMethod.PATCH, value = "/hellop")
		public ResponseEntity<Void> patchHello();
	}

	protected static interface UserService {

		@RequestMapping(method = RequestMethod.GET, value ="/users/{id}")
		User getUser(@PathVariable("id") long id);
	}

	@FeignClient("localapp")
	protected static interface UserClient extends UserService {

	}

	@Configuration
	@EnableAutoConfiguration
	@RestController
	@EnableFeignClients
	@RibbonClient(name = "localapp", configuration = LocalRibbonClientConfiguration.class)
	protected static class Application implements UserService {

		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		public Hello getHello() {
			return new Hello("hello world 1");
		}

		@RequestMapping(method = RequestMethod.PATCH, value = "/hellop")
		public ResponseEntity<Void> patchHello() {
			return ResponseEntity.ok().header("X-Hello", "hello world patch").build();
		}

		@Override
		public User getUser(@PathVariable("id") long id) {
			return new User("John Smith");
		}

		public static void main(String[] args) {
			new SpringApplicationBuilder(Application.class).properties(
					"spring.application.name=feignclienttest",
					"management.contextPath=/admin").run(args);
		}
	}

	@Test
	public void testSimpleType() {
		Hello hello = this.testClient.getHello();
		assertNotNull("hello was null", hello);
		assertEquals("first hello didn't match", new Hello("hello world 1"), hello);
	}

	@Test
	public void testPatch() {
		ResponseEntity<Void> response = this.testClient.patchHello();
		assertThat(response, is(notNullValue()));
		String header = response.getHeaders().getFirst("X-Hello");
		assertThat(header, equalTo("hello world patch"));
	}

	@Test
	public void testFeignClientType() throws IllegalAccessException {
		assertThat(this.feignClient, is(instanceOf(feign.ribbon.RibbonClient.class)));
		Field field = ReflectionUtils.findField(feign.ribbon.RibbonClient.class,
				"delegate", Client.class);
		ReflectionUtils.makeAccessible(field);
		Client delegate = (Client) field.get(this.feignClient);
		assertThat(delegate, is(instanceOf(feign.httpclient.ApacheHttpClient.class)));
	}

	@Test
	public void testUserCliet() {
		assertNotNull("UserClient was null", userClient);
		final User user = userClient.getUser(1);
		assertNotNull("Returned user was null", user);
		assertEquals("Users were different", user, new User("John Smith"));
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class Hello {
		private String message;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class User {
		private String name;
	}

	// Load balancer with fixed server list for "local" pointing to localhost
	@Configuration
	static class LocalRibbonClientConfiguration {

		@Value("${local.server.port}")
		private int port = 0;

		@Bean
		public ILoadBalancer ribbonLoadBalancer() {
			BaseLoadBalancer balancer = new BaseLoadBalancer();
			balancer.setServersList(Arrays.asList(new Server("localhost", this.port)));
			return balancer;
		}

	}
}
