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
import java.util.Arrays;

import lombok.SneakyThrows;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = RibbonClientHttpRequestFactoryTests.App.class)
@WebIntegrationTest(value = { "spring.application.name=ribbonclienttest",
		"spring.jmx.enabled=true" }, randomPort = true)
@DirtiesContext
public class RibbonClientHttpRequestFactoryTests {

	@Autowired
	private RestTemplate restTemplate;

	@Test
	public void requestFactoryIsRibbon() {
		assertTrue("wrong RequestFactory type", restTemplate.getRequestFactory() instanceof RibbonClientHttpRequestFactory);
	}

	@Test
	public void vanillaRequestWorks() {
		ResponseEntity<String> response = restTemplate.getForEntity("http://simple/",
				String.class);
		assertEquals("wrong response code", HttpStatus.OK, response.getStatusCode());
		assertEquals("wrong response body", "hello", response.getBody());
	}

	@Test
	public void requestWithPathParamWorks() {
		ResponseEntity<String> response = restTemplate.getForEntity("http://simple/path/{param}",
				String.class, "world");
		assertEquals("wrong response code", HttpStatus.OK, response.getStatusCode());
		assertEquals("wrong response body", "hello world", response.getBody());
	}

	@Test
	public void requestWithRequestParamWorks() {
		ResponseEntity<String> response = restTemplate.getForEntity("http://simple/request?param={param}", String.class, "world");
		assertEquals("wrong response code", HttpStatus.OK, response.getStatusCode());
		assertEquals("wrong response body", "hello world", response.getBody());
	}

	@Test
	public void requestWithPostWorks() {
		ResponseEntity<String> response = restTemplate.postForEntity("http://simple/post", "world", String.class);
		assertEquals("wrong response code", HttpStatus.OK, response.getStatusCode());
		assertEquals("wrong response body", "hello world", response.getBody());
	}

	@Test
	@SneakyThrows
	public void requestWithHeaderWorks() {
		RequestEntity<Void> entity = RequestEntity.get(new URI("http://simple/header"))
				.header("X-Param", "world")
				.build();
		ResponseEntity<String> response = restTemplate.exchange(entity, String.class);
		assertEquals("wrong response code", HttpStatus.OK, response.getStatusCode());
		assertEquals("wrong response body", "hello world", response.getBody());
	}

	@Configuration
	@EnableAutoConfiguration
	@RestController
	@RibbonClient(value = "simple", configuration = SimpleRibbonClientConfiguration.class)
	protected static class App {

		@RequestMapping("/")
		public String hi() {
			return "hello";
		}

		@RequestMapping("/path/{param}")
		public String hiParam(@PathVariable("param") String param) {
			return "hello "+param;
		}

		@RequestMapping("/request")
		public String hiRequest(@RequestParam("param") String param) {
			return "hello "+param;
		}

		@RequestMapping(value = "/post", method = RequestMethod.POST)
		public String hiPost(@RequestBody String param) {
			return "hello "+param;
		}

		@RequestMapping("/header")
		public String hiHeader(@RequestHeader("X-Param") String param) {
			return "hello "+param;
		}
	}
}

@Configuration
class SimpleRibbonClientConfiguration {

	@Value("${local.server.port}")
	private int port = 0;

	@Bean
	public ILoadBalancer ribbonLoadBalancer() {
		BaseLoadBalancer balancer = new BaseLoadBalancer();
		balancer.setServersList(Arrays.asList(new Server("localhost", port)));
		return balancer;
	}

}