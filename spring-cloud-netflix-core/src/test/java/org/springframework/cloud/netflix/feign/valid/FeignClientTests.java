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

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.support.FallbackCommand;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.cloud.netflix.feign.ribbon.LoadBalancerFeignClient;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;

import feign.Client;
import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import rx.Observable;

/**
 * @author Spencer Gibb
 * @author Jakub Narloch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = FeignClientTests.Application.class)
@WebIntegrationTest(randomPort = true, value = {
		"spring.application.name=feignclienttest",
		"logging.level.org.springframework.cloud.netflix.feign.valid=DEBUG",
		"feign.httpclient.enabled=false", "feign.okhttp.enabled=false"})
@DirtiesContext
public class FeignClientTests {

	@Value("${local.server.port}")
	private int port = 0;

	@Autowired
	private TestClient testClient;

	@Autowired
	private TestClientServiceId testClientServiceId;

	@Autowired
	private DecodingTestClient decodingTestClient;

	@Autowired
	private Client feignClient;

	@Autowired
	HystrixClient hystrixClient;

	protected enum Arg {
		A, B;

		@Override
		public String toString() {
			return name().toLowerCase(Locale.ENGLISH);
		}
	}

	@FeignClient(value = "localapp", configuration = TestClientConfig.class)
	protected interface TestClient {
		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		Hello getHello();

		@RequestMapping(method = RequestMethod.GET, value = "/hellos")
		List<Hello> getHellos();

		@RequestMapping(method = RequestMethod.GET, value = "/hellostrings")
		List<String> getHelloStrings();

		@RequestMapping(method = RequestMethod.GET, value = "/helloheaders")
		List<String> getHelloHeaders();

		@RequestMapping(method = RequestMethod.GET, value = "/helloparams")
		List<String> getParams(@RequestParam("params") List<String> params);

		@RequestMapping(method = RequestMethod.GET, value = "/hellos")
		HystrixCommand<List<Hello>> getHellosHystrix();

		@RequestMapping(method = RequestMethod.GET, value = "/noContent")
		ResponseEntity noContent();

		@RequestMapping(method = RequestMethod.HEAD, value = "/head")
		ResponseEntity head();

		@RequestMapping(method = RequestMethod.GET, value = "/tostring")
		String getToString(@RequestParam("arg") Arg arg);
	}

	public static class TestClientConfig {

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
	}

	@FeignClient(name = "localapp1")
	protected interface TestClientServiceId {
		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		Hello getHello();
	}

	@FeignClient(name = "localapp2", decode404 = true)
	protected interface DecodingTestClient {
		@RequestMapping(method = RequestMethod.GET, value = "/notFound")
		ResponseEntity<String> notFound();
	}

	@FeignClient(name = "localapp3", fallback = HystrixClientFallback.class)
	protected interface HystrixClient {
		@RequestMapping(method = RequestMethod.GET, value = "/fail")
		Hello fail();

		@RequestMapping(method = RequestMethod.GET, value = "/fail")
		HystrixCommand<Hello> failCommand();

		@RequestMapping(method = RequestMethod.GET, value = "/fail")
		Observable<Hello> failObservable();

		@RequestMapping(method = RequestMethod.GET, value = "/fail")
		Future<Hello> failFuture();
	}

	static class HystrixClientFallback implements HystrixClient {
		@Override
		public Hello fail() {
			return new Hello("fallback");
		}

		@Override
		public HystrixCommand<Hello> failCommand() {
			return new FallbackCommand<>(new Hello("fallbackcommand"));
		}

		@Override
		public Observable<Hello> failObservable() {
			return new FallbackCommand<>(new Hello("fallbackobservable")).observe();
		}

		@Override
		public Future<Hello> failFuture() {
			return new FallbackCommand<>(new Hello("fallbackfuture")).queue();
		}
	}

	@Configuration
	@EnableAutoConfiguration
	@RestController
	@EnableFeignClients(clients = {TestClientServiceId.class, TestClient.class, DecodingTestClient.class, HystrixClient.class},
			defaultConfiguration = TestDefaultFeignConfig.class)
	@RibbonClients({
			@RibbonClient(name = "localapp", configuration = LocalRibbonClientConfiguration.class),
			@RibbonClient(name = "localapp1", configuration = LocalRibbonClientConfiguration.class),
			@RibbonClient(name = "localapp2", configuration = LocalRibbonClientConfiguration.class),
			@RibbonClient(name = "localapp3", configuration = LocalRibbonClientConfiguration.class),
	})
	protected static class Application {


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

		@RequestMapping(method = RequestMethod.GET, value = "/helloparams")
		public List<String> getParams(@RequestParam("params") List<String> params) {
			return params;
		}

		@RequestMapping(method = RequestMethod.GET, value = "/noContent")
		ResponseEntity noContent() {
			return ResponseEntity.noContent().build();
		}

		@RequestMapping(method = RequestMethod.HEAD, value = "/head")
		ResponseEntity head() {
			return ResponseEntity.ok().build();
		}

		@RequestMapping(method = RequestMethod.GET, value = "/fail")
		String fail() {
			throw new RuntimeException("always fails");
		}

		@RequestMapping(method = RequestMethod.GET, value = "/notFound")
		ResponseEntity notFound() {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body((String)null);
		}

		@RequestMapping(method = RequestMethod.GET, value = "/tostring")
		String getToString(@RequestParam("arg") Arg arg) {
			return arg.toString();
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
		assertThat(this.feignClient, is(instanceOf(LoadBalancerFeignClient.class)));
		LoadBalancerFeignClient client = (LoadBalancerFeignClient) this.feignClient;
		Client delegate = client.getDelegate();
		assertThat(delegate, is(instanceOf(feign.Client.Default.class)));
	}

	@Test
	public void testServiceId() {
		assertNotNull("testClientServiceId was null", this.testClientServiceId);
		final Hello hello = this.testClientServiceId.getHello();
		assertNotNull("The hello response was null", hello);
		assertEquals("first hello didn't match", new Hello("hello world 1"), hello);
	}

	@Test
	public void testParams() {
		List<String> list = Arrays.asList("a", "1", "test");
		List<String> params = this.testClient.getParams(list);
		assertNotNull("params was null", params);
		assertEquals("params size was wrong", list.size(), params.size());
	}

	@Test
	public void testHystrixCommand() {
		HystrixCommand<List<Hello>> command = this.testClient.getHellosHystrix();
		assertNotNull("command was null", command);
		assertEquals("Hystrix command group name should match the name of the feign client", "localapp", command.getCommandGroup().name());
		List<Hello> hellos = command.execute();
		assertNotNull("hellos was null", hellos);
		assertEquals("hellos didn't match", hellos, getHelloList());
	}

	@Test
	public void testNoContentResponse() {
		ResponseEntity response = testClient.noContent();
		assertNotNull("response was null", response);
		assertEquals("status code was wrong", HttpStatus.NO_CONTENT, response.getStatusCode());
	}

	@Test
	public void testHeadResponse() {
		ResponseEntity response = testClient.head();
		assertNotNull("response was null", response);
		assertEquals("status code was wrong", HttpStatus.OK, response.getStatusCode());
	}

	@Test
	public void testDecodeNotFound() {
		ResponseEntity response = decodingTestClient.notFound();
		assertNotNull("response was null", response);
		assertEquals("status code was wrong", HttpStatus.NOT_FOUND, response.getStatusCode());
		assertNull("response body was not null", response.getBody());
	}

	@Test
	public void testConvertingExpander() {
		assertEquals(Arg.A.toString(), testClient.getToString(Arg.A));
		assertEquals(Arg.B.toString(), testClient.getToString(Arg.B));
	}

	@Test
	public void testHystrixFallbackWorks() {
		Hello hello = hystrixClient.fail();
		assertNotNull("hello was null", hello);
		assertEquals("message was wrong", "fallback", hello.getMessage());
	}

	@Test
	@Ignore("Until HystrixCommand works in fallback")
	public void testHystrixFallbackCommand() {
		HystrixCommand<Hello> command = hystrixClient.failCommand();
		assertNotNull("command was null", command);
		Hello hello = command.execute();
		assertNotNull("hello was null", hello);
		assertEquals("message was wrong", "fallbackcommand", hello.getMessage());
	}

	@Test
	@Ignore("Until Observable works in fallback")
	public void testHystrixFallbackObservable() {
		Observable<Hello> observable = hystrixClient.failObservable();
		assertNotNull("observable was null", observable);
		Hello hello = observable.toBlocking().first();
		assertNotNull("hello was null", hello);
		assertEquals("message was wrong", "fallbackobservable", hello.getMessage());
	}

	@Test
	public void testHystrixFallbackFuture() throws Exception {
		Future<Hello> future = hystrixClient.failFuture();
		assertNotNull("future was null", future);
		Hello hello = future.get(1, TimeUnit.SECONDS);
		assertNotNull("hello was null", hello);
		assertEquals("message was wrong", "fallbackfuture", hello.getMessage());
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class Hello {
		private String message;
	}

	@Configuration
	public static class TestDefaultFeignConfig {
		@Bean
		Logger.Level feignLoggerLevel() {
			return Logger.Level.FULL;
		}

		@Bean
		public HystrixClientFallback hystrixClientFallback() {
			return new HystrixClientFallback();
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
