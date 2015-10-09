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
	FeignClientFactory factory;

	@Value("${local.server.port}")
	private int port = 0;

	public SpringDecoderTests() {
		setName("test");
	}

	public TestClient testClient() {
		setType(this.getClass());
		return feign(factory).target(TestClient.class, "http://localhost:" + this.port);
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

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class Hello {
		private String message;
	}

	protected static interface TestClient {
		@RequestMapping(method = RequestMethod.GET, value = "/helloresponse")
		public ResponseEntity<Hello> getHelloResponse();

		@RequestMapping(method = RequestMethod.GET, value = "/hellovoid")
		public ResponseEntity<Void> getHelloVoid();

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

		public static void main(String[] args) {
			new SpringApplicationBuilder(Application.class).properties(
					"spring.application.name=springdecodertest",
					"management.contextPath=/admin").run(args);
		}
	}

}
