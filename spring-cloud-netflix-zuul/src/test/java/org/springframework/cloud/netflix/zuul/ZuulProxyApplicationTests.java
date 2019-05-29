/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.zuul;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.zuul.context.RequestContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.cloud.netflix.zuul.test.NoSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ZuulProxyApplicationTests.ZuulProxyApplication.class,
		webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "zuul.routes.simplezpat:/simplezpat/**",
				"logging.level.org.apache.http: DEBUG" })
@DirtiesContext
public class ZuulProxyApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate testRestTemplate;

	@Before
	public void setTestRequestcontext() {
		RequestContext context = new RequestContext();
		RequestContext.testSetCurrentContext(context);
	}

	@After
	public void clear() {
		RequestContext.getCurrentContext().clear();
	}

	@Test
	public void getHasCorrectTransferEncoding() {
		ResponseEntity<String> result = testRestTemplate.getForEntity(url(),
				String.class);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isEqualTo("missing");
	}

	@Test
	public void postHasCorrectTransferEncoding() {
		ResponseEntity<String> result = testRestTemplate.postForEntity(url(),
				new HttpEntity<>("hello"), String.class);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isEqualTo("missing");
	}

	@Test
	public void preflightRequestSucceedsForGetRequest() {
		MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		headers.put("Origin", singletonList("https://hello.com"));
		headers.put("Access-Control-Request-Method", singletonList("GET"));
		ResponseEntity<Void> result = testRestTemplate.exchange(url(), HttpMethod.OPTIONS,
				new HttpEntity<>(headers), Void.class);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	public void preflightRequestIsForbiddenForUnsupportedMethod() {
		MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		headers.put("Origin", singletonList("https://hello.com"));
		headers.put("Access-Control-Request-Method", singletonList("PUT"));
		ResponseEntity<Void> result = testRestTemplate.exchange(url(), HttpMethod.OPTIONS,
				new HttpEntity<>(headers), Void.class);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	public void preflightRequestIsForbiddenForUnsupportedorigin() {
		MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		headers.put("Origin", singletonList("http://unknown-origin.com"));
		headers.put("Access-Control-Request-Method", singletonList("GET"));
		ResponseEntity<Void> result = testRestTemplate.exchange(url(), HttpMethod.OPTIONS,
				new HttpEntity<>(headers), Void.class);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	private String url() {
		return "http://localhost:" + this.port + "/simplezpat/transferencoding";
	}

	// Don't use @SpringBootApplication because we don't want to component scan
	@Configuration
	@EnableAutoConfiguration
	@RestController
	@EnableZuulProxy
	@RibbonClient(name = "simplezpat",
			configuration = TestRibbonClientConfiguration.class)
	@Import(NoSecurityConfiguration.class)
	static class ZuulProxyApplication {

		@RequestMapping(value = "/transferencoding", method = RequestMethod.GET)
		public String get(@RequestHeader(name = "Transfer-Encoding",
				required = false) String transferEncoding) {
			if (transferEncoding == null) {
				return "missing";
			}
			return transferEncoding;
		}

		@RequestMapping(value = "/transferencoding", method = RequestMethod.POST)
		public String post(
				@RequestHeader(name = "Transfer-Encoding",
						required = false) String transferEncoding,
				@RequestBody String hello) {
			if (transferEncoding == null) {
				return "missing";
			}
			return transferEncoding;
		}

		@Bean
		public WebMvcConfigurer corsConfigurer() {
			return new WebMvcConfigurer() {
				public void addCorsMappings(CorsRegistry registry) {
					registry.addMapping("/simplezpat/**")
							.allowedOrigins("https://hello.com")
							.allowedMethods("GET", "POST")
							.allowedHeaders("Authorization");
				}
			};
		}

	}

	// Load balancer with fixed server list for "simplezpat" pointing to localhost
	@Configuration
	static class TestRibbonClientConfiguration {

		@LocalServerPort
		private int port;

		@Bean
		public ServerList<Server> ribbonServerList() {
			return new StaticServerList<>(new Server("localhost", this.port));
		}

	}

}
