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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;

import feign.Client;
import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = FeignClientTests.Application.class)
@WebAppConfiguration
@IntegrationTest({ "server.port=0", "spring.application.name=feignclienttest",
	"feign.httpclient.enabled=false"})
@DirtiesContext
public class FeignClientTests {

	@Value("${local.server.port}")
	private int port = 0;

	@Autowired
	private TestClient testClient;

	@Autowired
	private TestClientServiceId testClientServiceId;

	@Autowired
	private Client feignClient;

	// @FeignClient(value = "http://localhost:9876", loadbalance = false)
	@FeignClient("localapp")
	protected static interface TestClient {
		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		public Hello getHello();

		@RequestMapping(method = RequestMethod.GET, value = "/hellos")
		public List<Hello> getHellos();

		@RequestMapping(method = RequestMethod.GET, value = "/hellostrings")
		public List<String> getHelloStrings();

		@RequestMapping(method = RequestMethod.GET, value = "/helloheaders")
		public List<String> getHelloHeaders();
	}

	@FeignClient(serviceId = "localapp")
	protected static interface TestClientServiceId {
		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		public Hello getHello();
	}

	@Configuration
	@EnableAutoConfiguration
	@RestController
	@EnableFeignClients
	@RibbonClient(name = "localapp", configuration = LocalRibbonClientConfiguration.class)
	protected static class Application {

		@Bean
		public RequestInterceptor interceptor1() {
			return new RequestInterceptor() {
				@Override
				public void apply(RequestTemplate template) {
					template.header("myheader1", "myheader1value");
				}
			};
		}

		@Bean
		public RequestInterceptor interceptor2() {
			return new RequestInterceptor() {
				@Override
				public void apply(RequestTemplate template) {
					template.header("myheader2", "myheader2value");
				}
			};
		}

		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		public Hello getHello() {
			return new Hello("hello world 1");
		}

		@RequestMapping(method = RequestMethod.GET, value = "/hellos")
		public List<Hello> getHellos() {
			ArrayList<Hello> hellos = getHelloList();
			return hellos;
		}

		@RequestMapping(method = RequestMethod.GET, value = "/hellostrings")
		public List<String> getHelloStrings() {
			ArrayList<String> hellos = new ArrayList<>();
			hellos.add("hello world 1");
			hellos.add("oi terra 2");
			return hellos;
		}

		@RequestMapping(method = RequestMethod.GET, value = "/helloheaders")
		public List<String> getHelloHeaders(@RequestHeader("myheader1") String myheader1,
				@RequestHeader("myheader2") String myheader2) {
			ArrayList<String> headers = new ArrayList<>();
			headers.add(myheader1);
			headers.add(myheader2);
			return headers;
		}

		public static void main(String[] args) {
			new SpringApplicationBuilder(Application.class).properties(
					"spring.application.name=feignclienttest",
					"management.contextPath=/admin").run(args);
		}
	}

	private static ArrayList<Hello> getHelloList() {
		ArrayList<Hello> hellos = new ArrayList<>();
		hellos.add(new Hello("hello world 1"));
		hellos.add(new Hello("oi terra 2"));
		return hellos;
	}

	@Test
	public void testClient() {
		assertNotNull("testClient was null", this.testClient);
		assertTrue("testClient is not a java Proxy",
				Proxy.isProxyClass(this.testClient.getClass()));
		InvocationHandler invocationHandler = Proxy.getInvocationHandler(this.testClient);
		assertNotNull("invocationHandler was null", invocationHandler);
	}

	@Test
	public void testSimpleType() {
		Hello hello = this.testClient.getHello();
		assertNotNull("hello was null", hello);
		assertEquals("first hello didn't match", new Hello("hello world 1"), hello);
	}

	@Test
	public void testGenericType() {
		List<Hello> hellos = this.testClient.getHellos();
		assertNotNull("hellos was null", hellos);
		assertEquals("hellos didn't match", hellos, getHelloList());
	}

	@Test
	public void testRequestInterceptors() {
		List<String> headers = this.testClient.getHelloHeaders();
		assertNotNull("headers was null", headers);
		assertTrue("headers didn't contain myheader1value",
				headers.contains("myheader1value"));
		assertTrue("headers didn't contain myheader2value",
				headers.contains("myheader2value"));
	}

	@Test
	public void testFeignClientType() throws IllegalAccessException {
		assertThat(this.feignClient, is(instanceOf(feign.ribbon.RibbonClient.class)));
		Field field = ReflectionUtils.findField(feign.ribbon.RibbonClient.class, "delegate", Client.class);
		ReflectionUtils.makeAccessible(field);
		Client delegate = (Client) field.get(this.feignClient);
		assertThat(delegate, is(instanceOf(feign.Client.Default.class)));
	}

	@Test
	public void testServiceId() {
		assertNotNull("testClientServiceId was null", testClientServiceId);
		final Hello hello = testClientServiceId.getHello();
		assertNotNull("The hello response was null", hello);
		assertEquals("first hello didn't match", new Hello("hello world 1"), hello);
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class Hello {
		private String message;
	}
}

// Load balancer with fixed server list for "local" pointing to localhost
@Configuration
class LocalRibbonClientConfiguration {

	@Value("${local.server.port}")
	private int port = 0;

	@Bean
	public ILoadBalancer ribbonLoadBalancer() {
		BaseLoadBalancer balancer = new BaseLoadBalancer();
		balancer.setServersList(Arrays.asList(new Server("localhost", this.port)));
		return balancer;
	}

}
