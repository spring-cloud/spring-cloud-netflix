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

package org.springframework.cloud.netflix.feign.ribbon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = FeignRibbonClientRetryTests.Application.class)
@WebAppConfiguration
@IntegrationTest({ "server.port=0", "spring.application.name=feignclienttest",
		"localapp.ribbon.MaxAutoRetries=5", "localapp.ribbon.MaxAutoRetriesNextServer=5",
		"localapp.ribbon.OkToRetryOnAllOperations=true", })
@DirtiesContext
public class FeignRibbonClientRetryTests {

	@Value("${local.server.port}")
	private int port = 0;

	@Autowired
	private TestClient testClient;

	// @FeignClient(value = "http://localhost:9876", loadbalance = false)
	@FeignClient("localapp")
	protected static interface TestClient {
		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		public Hello getHello();

		@RequestMapping(method = RequestMethod.GET, value = "/retryme")
		public int retryMe();
	}

	@Configuration
	@EnableAutoConfiguration
	@RestController
	@EnableFeignClients
	@RibbonClient(name = "localapp", configuration = LocalRibbonClientConfiguration.class)
	public static class Application {

		private AtomicInteger retries = new AtomicInteger(1);

		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		public Hello getHello() {
			return new Hello("hello world 1");
		}

		@RequestMapping(method = RequestMethod.GET, value = "/retryme")
		public int retryMe() {
			return this.retries.getAndIncrement();
		}

		public static void main(String[] args) throws InterruptedException {
			new SpringApplicationBuilder(Application.class).properties(
					"spring.application.name=feignclienttest",
					"localapp.ribbon.MaxAutoRetries=5",
					"localapp.ribbon.MaxAutoRetriesNextServer=5",
					"localapp.ribbon.OkToRetryOnAllOperations=true",
					"management.contextPath=/admin"
			// ,"local.server.port=9999"
					).run(args);
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

	@Test
	public void testRetries() {
		int retryMe = this.testClient.retryMe();
		assertEquals("retryCount didn't match", retryMe, 1);
		// TODO: not sure how to verify retry happens. Debugging through it, it works
		// maybe the assertEquals above is enough because of the bogus servers
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class Hello {
		private String message;
	}
}

// Load balancer with fixed server list for "local" pointing to localhost
// some bogus servers are thrown in to test retry
@Configuration
class LocalRibbonClientConfiguration {

	@Value("${local.server.port}")
	private int port = 0;

	@Bean
	public ServerList<Server> ribbonServerList() {
		return new StaticServerList<>(new Server("___mybadhost__", 10001),
				new Server("___mybadhost2__", 10002),
				new Server("___mybadhost3__", 10003), new Server("localhost", this.port));
	}

}
