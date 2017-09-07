/*
 * Copyright 2013-2016 the original author or authors.
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

package org.springframework.cloud.netflix.feign;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Field;
import java.util.Objects;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.SocketUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import feign.Client;
import feign.Feign;
import feign.Target;
import feign.httpclient.ApacheHttpClient;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = FeignHttpClientUrlTests.TestConfig.class, webEnvironment = WebEnvironment.DEFINED_PORT, value = {
		"spring.application.name=feignclienturltest", "feign.hystrix.enabled=false",
		"feign.okhttp.enabled=false" })
@DirtiesContext
public class FeignHttpClientUrlTests {

	@BeforeClass
	public static void beforeClass() {
		int port = SocketUtils.findAvailableTcpPort();
		System.setProperty("server.port", String.valueOf(port));
	}

	@AfterClass
	public static void afterClass() {
		System.clearProperty("server.port");
	}

	@Autowired
	private UrlClient urlClient;

	@Autowired
	private BeanUrlClient beanClient;

	@Autowired BeanUrlClientNoProtocol beanClientNoProtocol;

	// this tests that
	@FeignClient(name = "localappurl", url = "http://localhost:${server.port}/")
	protected interface UrlClient {
		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		Hello getHello();
	}

	@FeignClient(name = "beanappurl", url = "#{SERVER_URL}path")
	protected interface BeanUrlClient {
		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		Hello getHello();
	}

	@FeignClient(name = "beanappurlnoprotocol", url = "#{SERVER_URL_NO_PROTOCOL}path")
	protected interface BeanUrlClientNoProtocol {
		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		Hello getHello();
	}

	@Configuration
	@EnableAutoConfiguration
	@RestController
	@EnableFeignClients(clients = { UrlClient.class, BeanUrlClient.class, BeanUrlClientNoProtocol.class })
	protected static class TestConfig {
		@Value("${server.port}")
		private int port;

		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		public Hello getHello() {
			return new Hello("hello world 1");
		}

		@RequestMapping(method = RequestMethod.GET, value = "/path/hello")
		public Hello getHelloWithPath() {
			return getHello();
		}

		@Bean(name="SERVER_URL")
		public String serverUrl() {
			return "http://localhost:" + port + "/";
		}

		@Bean(name="SERVER_URL_NO_PROTOCOL")
		public String serverUrlNoProtocol() {
			return "localhost:" + port + "/";
		}

		@Bean
		public Targeter feignTargeter() {
			return new Targeter() {
				@Override
				public <T> T target(FeignClientFactoryBean factory, Feign.Builder feign,
						FeignContext context, Target.HardCodedTarget<T> target) {
					Field field = ReflectionUtils.findField(Feign.Builder.class,
							"client");
					ReflectionUtils.makeAccessible(field);
					Client client = (Client) ReflectionUtils.getField(field, feign);
					if (target.name().equals("localappurl")) {
						assertThat("client was wrong type", client,
								is(instanceOf(ApacheHttpClient.class)));
					}
					return feign.target(target);
				}
			};
		}

	}

	@Test
	public void testUrlHttpClient() {
		assertNotNull("UrlClient was null", this.urlClient);
		Hello hello = this.urlClient.getHello();
		assertNotNull("hello was null", hello);
		assertEquals("first hello didn't match", new Hello("hello world 1"), hello);
	}

	@Test
	public void testBeanUrl() {
		Hello hello = this.beanClient.getHello();
		assertNotNull("hello was null", hello);
		assertEquals("first hello didn't match", new Hello("hello world 1"), hello);
	}

	@Test
	public void testBeanUrlNoProtocol() {
		Hello hello = this.beanClientNoProtocol.getHello();
		assertNotNull("hello was null", hello);
		assertEquals("first hello didn't match", new Hello("hello world 1"), hello);
	}

	public static class Hello {
		private String message;

		public Hello() {
		}

		public Hello(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Hello that = (Hello) o;
			return Objects.equals(message, that.message);
		}

		@Override
		public int hashCode() {
			return Objects.hash(message);
		}
	}
}
