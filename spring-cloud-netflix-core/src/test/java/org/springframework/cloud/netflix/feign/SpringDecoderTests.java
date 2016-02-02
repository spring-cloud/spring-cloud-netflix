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

package org.springframework.cloud.netflix.feign;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SpringDecoderTests.Application.class)
@WebAppConfiguration
@IntegrationTest({ "server.port=0", "spring.application.name=springdecodertest",
		"spring.jmx.enabled=true" })
@DirtiesContext
public class SpringDecoderTests extends FeignClientFactoryBean {

	@Autowired
	FeignContext context;

	@Value("${local.server.port}")
	private int port = 0;

	public SpringDecoderTests() {
		setName("test");
	}

	public TestClient testClient() {
		return testClient(false);
	}

	public TestClient testClient(boolean decode404) {
		setType(this.getClass());
		setDecode404(decode404);
		return feign(context).target(TestClient.class, "http://localhost:" + this.port);
	}

	@Test
	public void testResponseEntity() {
		ResponseEntity<Hello> response = testClient().getHelloResponse();
		assertNotNull("response was null", response);
		assertEquals("wrong status code", HttpStatus.OK, response.getStatusCode());
		Hello hello = response.getBody();
		assertNotNull("hello was null", hello);
		assertEquals("first hello didn't match", new Hello("hello world via response"), hello);
	}

	@Test
	public void testSimpleType() {
		Hello hello = testClient().getHello();
		assertNotNull("hello was null", hello);
		assertEquals("first hello didn't match", new Hello("hello world 1"), hello);
	}

	@Test
	public void testUserParameterizedTypeDecode() {
		List<Hello> hellos = testClient().getHellos();
		assertNotNull("hellos was null", hellos);
		assertEquals("hellos was not the right size", 2, hellos.size());
		assertEquals("first hello didn't match", new Hello("hello world 1"),
				hellos.get(0));
	}

	@Test
	public void testSimpleParameterizedTypeDecode() {
		List<String> hellos = testClient().getHelloStrings();
		assertNotNull("hellos was null", hellos);
		assertEquals("hellos was not the right size", 2, hellos.size());
		assertEquals("first hello didn't match", "hello world 1", hellos.get(0));
	}

	@Test
	public void testResponseEntityVoid() {
		ResponseEntity<Void> response = testClient().getHelloVoid();
		assertNotNull("response was null", response);
		List<String> headerVals = response.getHeaders().get("X-test-header");
		assertNotNull("headerVals was null", headerVals);
		assertEquals("headerVals size was wrong", 1, headerVals.size());
		String header = headerVals.get(0);
		assertEquals("header was wrong", "myval", header);
	}

	@Test(expected = RuntimeException.class)
	public void test404() {
		testClient().getNotFound();
	}

	@Test
	public void testDecodes404() {
		final ResponseEntity<String> response = testClient(true).getNotFound();
		assertNotNull("response was null", response);
		assertNull("response body was not null", response.getBody());
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
			return "org.springframework.cloud.netflix.feign.SpringDecoderTests.Hello(message="
					+ this.message + ")";
		}
	}

	protected interface TestClient {
		@RequestMapping(method = RequestMethod.GET, value = "/helloresponse")
		ResponseEntity<Hello> getHelloResponse();

		@RequestMapping(method = RequestMethod.GET, value = "/hellovoid")
		ResponseEntity<Void> getHelloVoid();

		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		Hello getHello();

		@RequestMapping(method = RequestMethod.GET, value = "/hellos")
		List<Hello> getHellos();

		@RequestMapping(method = RequestMethod.GET, value = "/hellostrings")
		List<String> getHelloStrings();

		@RequestMapping(method = RequestMethod.GET, value = "/hellonotfound")
		ResponseEntity<String> getNotFound();
	}

	@Configuration
	@EnableAutoConfiguration
	@RestController
	protected static class Application implements TestClient {

		@Override
		public ResponseEntity<Hello> getHelloResponse() {
			return ResponseEntity.ok(new Hello("hello world via response"));
		}

		@Override
		public ResponseEntity<Void> getHelloVoid() {
			return ResponseEntity.noContent().header("X-test-header", "myval").build();
		}

		@Override
		public Hello getHello() {
			return new Hello("hello world 1");
		}

		@Override
		public List<Hello> getHellos() {
			ArrayList<Hello> hellos = new ArrayList<>();
			hellos.add(new Hello("hello world 1"));
			hellos.add(new Hello("oi terra 2"));
			return hellos;
		}

		@Override
		public List<String> getHelloStrings() {
			ArrayList<String> hellos = new ArrayList<>();
			hellos.add("hello world 1");
			hellos.add("oi terra 2");
			return hellos;
		}

		@Override
		public ResponseEntity<String> getNotFound() {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body((String)null);
		}

		public static void main(String[] args) {
			new SpringApplicationBuilder(Application.class).properties(
					"spring.application.name=springdecodertest",
					"management.contextPath=/admin").run(args);
		}
	}

}
