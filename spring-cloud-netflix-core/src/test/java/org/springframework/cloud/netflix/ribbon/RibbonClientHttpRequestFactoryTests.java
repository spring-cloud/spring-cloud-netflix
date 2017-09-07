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

package org.springframework.cloud.netflix.ribbon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = RibbonClientHttpRequestFactoryTests.App.class, webEnvironment = WebEnvironment.RANDOM_PORT, value = {
		"spring.application.name=ribbonclienttest", "spring.jmx.enabled=true",
		"spring.cloud.netflix.metrics.enabled=false", "ribbon.http.client.enabled=true" })
@DirtiesContext
public class RibbonClientHttpRequestFactoryTests {

	@Rule
	public final ExpectedException exceptionRule = ExpectedException.none();

	@LoadBalanced
	@Autowired
	protected RestTemplate restTemplate;

	@Test
	public void requestFactoryIsRibbon() {
		ClientHttpRequestFactory requestFactory = this.restTemplate.getRequestFactory();
		assertTrue("wrong RequestFactory type: " + requestFactory.getClass(),
				requestFactory instanceof RibbonClientHttpRequestFactory);
	}

	@Test
	public void vanillaRequestWorks() {
		ResponseEntity<String> response = this.restTemplate.getForEntity("http://simple/",
				String.class);
		assertEquals("wrong response code", HttpStatus.OK, response.getStatusCode());
		assertEquals("wrong response body", "hello", response.getBody());
	}

	@Test
	public void requestWithPathParamWorks() {
		ResponseEntity<String> response = this.restTemplate
				.getForEntity("http://simple/path/{param}", String.class, "world");
		assertEquals("wrong response code", HttpStatus.OK, response.getStatusCode());
		assertEquals("wrong response body", "hello world", response.getBody());
	}

	@Test
	public void requestWithEncodedPathParamWorks() {
		ResponseEntity<String> response = this.restTemplate.getForEntity(
				"http://simple/path/{param}", String.class, "world & everyone else");
		assertEquals("wrong response code", HttpStatus.OK, response.getStatusCode());
		assertEquals("wrong response body", "hello world & everyone else",
				response.getBody());
	}

	@Test
	public void requestWithRequestParamWorks() {
		ResponseEntity<String> response = this.restTemplate.getForEntity(
				"http://simple/request?param={param}", String.class, "world");
		assertEquals("wrong response code", HttpStatus.OK, response.getStatusCode());
		assertEquals("wrong response body", "hello world", response.getBody());
	}

	@Test
	public void requestWithPostWorks() {
		ResponseEntity<String> response = this.restTemplate
				.postForEntity("http://simple/post", "world", String.class);
		assertEquals("wrong response code", HttpStatus.OK, response.getStatusCode());
		assertEquals("wrong response body", "hello world", response.getBody());
	}

	@Test
	public void requestWithEmptyPostWorks() {
		ResponseEntity<String> response = this.restTemplate
				.postForEntity("http://simple/emptypost", "", String.class);
		assertEquals("wrong response code", HttpStatus.OK, response.getStatusCode());
		assertEquals("wrong response body", "hello empty", response.getBody());
	}

	@Test
	public void requestWithHeaderWorks() throws Exception {
		RequestEntity<Void> entity = RequestEntity.get(new URI("http://simple/header"))
				.header("X-Param", "world").build();
		ResponseEntity<String> response = this.restTemplate.exchange(entity,
				String.class);
		assertEquals("wrong response code", HttpStatus.OK, response.getStatusCode());
		assertEquals("wrong response body", "hello world", response.getBody());
	}

	@Test
	public void invalidHostNameError() {
		this.exceptionRule.expect(ResourceAccessException.class);
		this.exceptionRule.expectMessage("Invalid hostname");
		this.restTemplate.getForEntity("http://simple_bad", String.class);
	}

	@Configuration
	@EnableAutoConfiguration
	@RestController
	@RibbonClient(value = "simple", configuration = SimpleRibbonClientConfiguration.class)
	public static class App {

		@LoadBalanced
		@Bean
		RestTemplate restTemplate() {
			return new RestTemplate();
		}

		@RequestMapping("/")
		public String hi() {
			return "hello";
		}

		@RequestMapping("/path/{param}")
		public String hiParam(@PathVariable("param") String param) {
			return "hello " + param;
		}

		@RequestMapping("/request")
		public String hiRequest(@RequestParam("param") String param) {
			return "hello " + param;
		}

		@RequestMapping(value = "/post", method = RequestMethod.POST)
		public String hiPost(@RequestBody String param) {
			return "hello " + param;
		}

		@RequestMapping(value = "/emptypost", method = RequestMethod.POST)
		public String hiPostEmpty() {
			return "hello empty";
		}

		@RequestMapping("/header")
		public String hiHeader(@RequestHeader("X-Param") String param) {
			return "hello " + param;
		}
	}

	@Configuration
	static class SimpleRibbonClientConfiguration {

		@Value("${local.server.port}")
		private int port = 0;

		@Bean
		public ServerList<Server> ribbonServerList() {
			return new StaticServerList<>(new Server("localhost", this.port));
		}

	}
}
