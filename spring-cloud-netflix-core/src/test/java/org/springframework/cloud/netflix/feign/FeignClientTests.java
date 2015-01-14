package org.springframework.cloud.netflix.feign;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
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
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = FeignClientTests.Application.class)
@WebAppConfiguration
@IntegrationTest({ "server.port=0", "spring.application.name=feignclienttest" })
@DirtiesContext
public class FeignClientTests extends FeignConfiguration {

	@Value("${local.server.port}")
	private int port = 0;

	@Autowired
	TestClient testClient;

	// @FeignClient(value = "http://localhost:9876", loadbalance = false)
	@FeignClient("feignclienttest")
	protected static interface TestClient {
		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		public Hello getHello();

		@RequestMapping(method = RequestMethod.GET, value = "/hellos")
		public List<Hello> getHellos();

		@RequestMapping(method = RequestMethod.GET, value = "/hellostrings")
		public List<String> getHelloStrings();
	}

	@Configuration
	@EnableAutoConfiguration
	@RestController
	@FeignClientScan
	protected static class Application {

		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		public Hello getHello() {
			return new Hello("hello world 1");
		}

		@RequestMapping(method = RequestMethod.GET, value = "/hellos")
		public List<Hello> getHellos() {
			ArrayList<Hello> hellos = new ArrayList<>();
			hellos.add(new Hello("hello world 1"));
			hellos.add(new Hello("oi terra 2"));
			return hellos;
		}

		@RequestMapping(method = RequestMethod.GET, value = "/hellostrings")
		public List<String> getHelloStrings() {
			ArrayList<String> hellos = new ArrayList<>();
			hellos.add("hello world 1");
			hellos.add("oi terra 2");
			return hellos;
		}

		public static void main(String[] args) {
			new SpringApplicationBuilder(Application.class).properties(
					"spring.application.name=feignclienttest",
					"management.contextPath=/admin").run(args);
		}
	}

	@Test
	public void testClient() {
		assertNotNull("testClient was null", this.testClient);
		assertTrue("testClient is not a java Proxy",
				Proxy.isProxyClass(this.testClient.getClass()));
		InvocationHandler invocationHandler = Proxy.getInvocationHandler(this.testClient);
		assertNotNull("invocationHandler was null", invocationHandler);
	}

	// TODO: only works if port is hardcoded cant resolve ${local.server.port} in
	// annotation
	/*
	 * @Test public void testSimpleType() { Hello hello = testClient.getHello();
	 * assertNotNull("hello was null", hello); assertEquals("first hello didn't match",
	 * new Hello("hello world 1"), hello); }
	 */

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class Hello {
		private String message;
	}
}
