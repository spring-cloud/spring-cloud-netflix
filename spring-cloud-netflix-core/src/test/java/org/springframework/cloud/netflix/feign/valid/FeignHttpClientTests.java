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

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;

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
import org.springframework.cloud.netflix.feign.ribbon.LoadBalancerFeignClient;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import feign.Client;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = FeignHttpClientTests.Application.class)
@WebAppConfiguration
@IntegrationTest({ "server.port=0", "spring.application.name=feignclienttest",
		"feign.hystrix.enabled=false", "feign.okhttp.enabled=false" })
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

	@FeignClient("localapp")
	protected interface TestClient extends BaseTestClient {
	}

	protected interface BaseTestClient {
		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		Hello getHello();

		@RequestMapping(method = RequestMethod.PATCH, value = "/hellop")
		ResponseEntity<Void> patchHello(Hello hello);
	}

	protected interface UserService {
		@RequestMapping(method = RequestMethod.GET, value = "/users/{id}")
		User getUser(@PathVariable("id") long id);
	}

	@FeignClient("localapp")
	protected interface UserClient extends UserService {
	}

	@Configuration
	@EnableAutoConfiguration
	@RestController
	@EnableFeignClients(clients = { TestClient.class, UserClient.class })
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
			new SpringApplicationBuilder(Application.class)
					.properties("spring.application.name=feignclienttest",
							"management.contextPath=/admin")
					.run(args);
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
		ResponseEntity<Void> response = this.testClient.patchHello(new Hello("foo"));
		assertThat(response, is(notNullValue()));
		String header = response.getHeaders().getFirst("X-Hello");
		assertThat(header, equalTo("hello world patch"));
	}

	@Test
	public void testFeignClientType() throws IllegalAccessException {
		assertThat(this.feignClient, is(instanceOf(LoadBalancerFeignClient.class)));
		LoadBalancerFeignClient client = (LoadBalancerFeignClient) this.feignClient;
		Client delegate = client.getDelegate();
		assertThat(delegate, is(instanceOf(feign.httpclient.ApacheHttpClient.class)));
	}

	@Test
	public void testFeignInheritanceSupport() {
		assertNotNull("UserClient was null", this.userClient);
		final User user = this.userClient.getUser(1);
		assertNotNull("Returned user was null", user);
		assertEquals("Users were different", user, new User("John Smith"));
	}

	public static class Hello {
		private String message;

		public Hello(String message) {
			this.message = message;
		}

		public Hello() {
		}

		public String getMessage() {
			return this.message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public boolean equals(Object o) {
			if (o == this)
				return true;
			if (!(o instanceof Hello))
				return false;
			final Hello other = (Hello) o;
			if (!other.canEqual((Object) this))
				return false;
			final Object this$message = this.message;
			final Object other$message = other.message;
			if (this$message == null ?
					other$message != null :
					!this$message.equals(other$message))
				return false;
			return true;
		}

		public int hashCode() {
			final int PRIME = 59;
			int result = 1;
			final Object $message = this.message;
			result = result * PRIME + ($message == null ? 0 : $message.hashCode());
			return result;
		}

		protected boolean canEqual(Object other) {
			return other instanceof Hello;
		}

		public String toString() {
			return "org.springframework.cloud.netflix.feign.valid.FeignHttpClientTests.Hello(message="
					+ this.message + ")";
		}
	}

	public static class User {
		private String name;

		public User(String name) {
			this.name = name;
		}

		public User() {
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public boolean equals(Object o) {
			if (o == this)
				return true;
			if (!(o instanceof User))
				return false;
			final User other = (User) o;
			if (!other.canEqual((Object) this))
				return false;
			final Object this$name = this.name;
			final Object other$name = other.name;
			if (this$name == null ? other$name != null : !this$name.equals(other$name))
				return false;
			return true;
		}

		public int hashCode() {
			final int PRIME = 59;
			int result = 1;
			final Object $name = this.name;
			result = result * PRIME + ($name == null ? 0 : $name.hashCode());
			return result;
		}

		protected boolean canEqual(Object other) {
			return other instanceof User;
		}

		public String toString() {
			return "org.springframework.cloud.netflix.feign.valid.FeignHttpClientTests.User(name="
					+ this.name + ")";
		}
	}

	// Load balancer with fixed server list for "local" pointing to localhost
	@Configuration
	static class LocalRibbonClientConfiguration {

		@Value("${local.server.port}")
		private int port = 0;

		@Bean
		public ServerList<Server> ribbonServerList() {
			return new StaticServerList<>(new Server("localhost", this.port));
		}

	}
}
