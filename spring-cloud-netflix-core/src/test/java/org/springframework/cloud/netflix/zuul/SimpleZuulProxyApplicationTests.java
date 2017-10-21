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

package org.springframework.cloud.netflix.zuul;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;

import com.netflix.zuul.context.RequestContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.netflix.zuul.filters.discovery.DiscoveryClientRouteLocator;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SimpleZuulProxyApplicationTests.SimpleZuulProxyApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT, value = {
		"zuul.forceOriginalQueryStringEncoding: true" })
@DirtiesContext
public class SimpleZuulProxyApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate testRestTemplate;

	@Autowired
	private DiscoveryClientRouteLocator routes;

	@Autowired
	private RoutesMvcEndpoint endpoint;

	@Before
	public void setTestRequestContext() {
		RequestContext context = new RequestContext();
		RequestContext.testSetCurrentContext(context);

		this.routes.addRoute("/foo/**", "http://localhost:" + this.port + "/bar");
		this.endpoint.reset();
	}

	@After
	public void clear() {
		RequestContext.getCurrentContext().clear();
	}

	@Test
	public void getOnSelfViaSimpleHostRoutingFilter() {
		ResponseEntity<String> result = executeSimpleRequest(HttpMethod.GET);

		assertResponseCodeAndBody(result, "get bar");
	}

	@Test
	public void postOnSelfViaSimpleHostRoutingFilter() {
		ResponseEntity<String> result = executeSimpleRequest(HttpMethod.POST);

		assertResponseCodeAndBody(result, "post bar");
	}

	@Test
	public void putOnSelfViaSimpleHostRoutingFilter() {
		ResponseEntity<String> result = executeSimpleRequest(HttpMethod.PUT);

		assertResponseCodeAndBody(result, "put bar");
	}

	@Test
	public void patchOnSelfViaSimpleHostRoutingFilter() {
		ResponseEntity<String> result = executeSimpleRequest(HttpMethod.PATCH);

		assertResponseCodeAndBody(result, "patch bar");
	}

	@Test
	public void deleteOnSelfViaSimpleHostRoutingFilter() {
		ResponseEntity<String> result = executeSimpleRequest(HttpMethod.DELETE);

		assertResponseCodeAndBody(result, "delete bar");
	}

	@Test
	public void getOnSelfWithComplexQueryParam() throws URISyntaxException {
		String encodedQueryString = "foo=%7B%22project%22%3A%22stream%22%2C%22logger%22%3A%22javascript%22%2C%22platform%22%3A%22javascript%22%2C%22request%22%3A%7B%22url%22%3A%22https%3A%2F%2Ffoo%2Fadmin";
		ResponseEntity<String> result = testRestTemplate.exchange(
				new URI("/foo?" + encodedQueryString), HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);

		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals(encodedQueryString, result.getBody());
	}

	private void assertResponseCodeAndBody(ResponseEntity<String> result,
			String expectedBody) {
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals(expectedBody, result.getBody());
	}

	private ResponseEntity<String> executeSimpleRequest(HttpMethod httpMethod) {
		ResponseEntity<String> result = testRestTemplate.exchange("/foo?id=bar",
				httpMethod, new HttpEntity<>((Void) null), String.class);
		return result;
	}

	// Don't use @SpringBootApplication because we don't want to component scan
	@Configuration
	@EnableAutoConfiguration
	@RestController
	@EnableZuulProxy
	static class SimpleZuulProxyApplication {

		@RequestMapping(value = "/bar", method = RequestMethod.GET)
		public String get(@RequestParam String id) {
			return "get " + id;
		}

		@RequestMapping(value = "/bar", method = RequestMethod.GET, params = { "foo" })
		public String complexGet(@RequestParam String foo, HttpServletRequest request) {
			return request.getQueryString();
		}

		@RequestMapping(value = "/bar", method = RequestMethod.POST)
		public String post(@RequestParam String id) {
			return "post " + id;
		}

		@RequestMapping(value = "/bar", method = RequestMethod.PUT)
		public String put(@RequestParam String id) {
			return "put " + id;
		}

		@RequestMapping(value = "/bar", method = RequestMethod.DELETE)
		public String delete(@RequestParam String id) {
			return "delete " + id;
		}

		@RequestMapping(value = "/bar", method = RequestMethod.PATCH)
		public String patch(@RequestParam String id) {
			return "patch " + id;
		}

	}
}
