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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

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
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;
import org.springframework.cloud.netflix.zuul.filters.discovery.DiscoveryClientRouteMatcher;
import org.springframework.cloud.netflix.zuul.routematcher.RouteCondition;
import org.springframework.cloud.netflix.zuul.routematcher.RouteOptions;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Sets;
import com.netflix.zuul.context.RequestContext;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = AdvancedZuulProxyApplicationTests.AdvancedZuulProxyApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT, value = {
		"zuul.forceOriginalQueryStringEncoding: true",
		"zuul.route-matcher.advanced: true" })
@DirtiesContext
public class AdvancedZuulProxyApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate testRestTemplate;

	@Autowired
	private DiscoveryClientRouteMatcher routes;

	@Autowired
	private RoutesMvcEndpoint endpoint;

	@Before
	public void setTestRequestContext() {
		RequestContext context = new RequestContext();
		RequestContext.testSetCurrentContext(context);

		List<RouteCondition> list = new ArrayList<>();
		list.add(new RouteCondition(String.format(
				"get-route=http://localhost:%d/bar-get,Method=GET", port)));

		list.add(new RouteCondition(String.format(
				"put-route=http://localhost:%d/bar-put,Method=PUT", port)));

		RouteOptions routeOptions = new RouteOptions();
		routeOptions.setAllowedMethods(Sets.newHashSet("GET", "PUT", "POST"));
		routeOptions.setRouteConditions(list);
		ZuulRoute zuulRoute = new ZuulRoute("foo", "/foo/**", null,
				"http://localhost:" + this.port + "/bar-default", true, false,
				null, routeOptions);

		this.routes.addRoute(zuulRoute);
		this.endpoint.reset();
	}

	@After
	public void clear() {
		RequestContext.getCurrentContext().clear();
	}

	@Test
	public void getCallShouldGoToItsOwnSpecifiedUrl() {
		ResponseEntity<String> result = executeSimpleRequest(HttpMethod.GET);

		assertResponseCodeAndBody(result, "GET route for bar called",
				HttpStatus.OK);
	}

	@Test
	public void postCallShouldGoToDefaultUrl() {
		ResponseEntity<String> result = executeSimpleRequest(HttpMethod.POST);

		assertResponseCodeAndBody(result, "Default route for bar called",
				HttpStatus.OK);
	}

	@Test
	public void putCallShouldGoToItsOwnSpecifiedUrl() {
		ResponseEntity<String> result = executeSimpleRequest(HttpMethod.PUT);

		assertResponseCodeAndBody(result, "PUT route for bar called",
				HttpStatus.OK);
	}

	@Test
	public void patchCallShouldResultIn404NotFound() {
		ResponseEntity<String> result = executeSimpleRequest(HttpMethod.PATCH);

		assertResponseCode(result, HttpStatus.NOT_FOUND);
	}

	private void assertResponseCodeAndBody(ResponseEntity<String> result,
			String expectedBody, HttpStatus expectedStatus) {
		assertEquals(expectedStatus, result.getStatusCode());
		assertEquals(expectedBody, result.getBody());
	}

	private void assertResponseCode(ResponseEntity<String> result,
			HttpStatus expectedStatus) {
		assertEquals(expectedStatus, result.getStatusCode());
	}

	private ResponseEntity<String> executeSimpleRequest(HttpMethod httpMethod) {
		ResponseEntity<String> result = testRestTemplate.exchange("/foo/bar",
				httpMethod, null, String.class);
		return result;
	}

	// Don't use @SpringBootApplication because we don't want to component scan
	@Configuration
	@EnableAutoConfiguration
	@RestController
	@EnableZuulProxy
	static class AdvancedZuulProxyApplication {

		@RequestMapping(value = "/bar-default/bar")
		public String defaultRoute() {
			return "Default route for bar called";
		}

		@RequestMapping(value = "/bar-get/bar", method = RequestMethod.GET)
		public String getRoute() {
			return "GET route for bar called";
		}

		@RequestMapping(value = "/bar-put/bar", method = RequestMethod.PUT)
		public String put() {
			return "PUT route for bar called";
		}
	}
}
