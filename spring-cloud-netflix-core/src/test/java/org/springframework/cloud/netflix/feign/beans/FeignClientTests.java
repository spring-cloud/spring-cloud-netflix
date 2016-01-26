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

package org.springframework.cloud.netflix.feign.beans;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = FeignClientTests.Application.class)
@WebIntegrationTest(randomPort = true, value = {
		"spring.application.name=feignclienttest",
		"logging.level.org.springframework.cloud.netflix.feign.valid=DEBUG",
		"feign.httpclient.enabled=false", "feign.okhttp.enabled=false" })
@DirtiesContext
public class FeignClientTests {

	@Value("${local.server.port}")
	private int port = 0;

	@Autowired
	private TestClient testClient;

	@Autowired
	private org.springframework.cloud.netflix.feign.beans.extra.TestClient extraClient;

	@Configuration
	@EnableAutoConfiguration
	@RestController
	@EnableFeignClients
	protected static class Application {

		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		public Hello getHello() {
			return new Hello("hello world 1");
		}

		public static void main(String[] args) {
			new SpringApplicationBuilder(Application.class)
					.properties("spring.application.name=feignclienttest",
							"management.contextPath=/admin")
					.run(args);
		}
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class Hello {
		private String message;
	}

	@Test
	public void testClient() {
		assertNotNull("testClient was null", this.testClient);
		assertNotNull("testClient was null", this.extraClient);
		assertTrue("testClient is not a java Proxy",
				Proxy.isProxyClass(this.testClient.getClass()));
		InvocationHandler invocationHandler = Proxy.getInvocationHandler(this.testClient);
		assertNotNull("invocationHandler was null", invocationHandler);
	}

	@Configuration
	public static class TestDefaultFeignConfig {
	}
}
