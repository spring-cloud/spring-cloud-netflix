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
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;
import org.springframework.cloud.netflix.zuul.filters.discovery.DiscoveryClientRouteLocator;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ContextPathZuulProxyApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT, value = {
		"server.contextPath: /app" })
@DirtiesContext
public class ContextPathZuulProxyApplicationTests {

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
	}

	@After
	public void clear() {
		RequestContext.getCurrentContext().clear();
	}

	@Test
	public void getOnSelfViaSimpleHostRoutingFilter() {
		this.routes.addRoute("/self/**", "http://localhost:" + this.port + "/app/local");
		this.endpoint.reset();
		ResponseEntity<String> result = testRestTemplate.exchange(
				"http://localhost:" + this.port + "/app/self/1", HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Gotten 1!", result.getBody());
	}

	@Test
	public void stripPrefixFalseAppendsPath() {
		this.routes.addRoute(new ZuulRoute("strip", "/strip/**", "strip",
				"http://localhost:" + this.port + "/app/local", false, false, null));
		this.endpoint.reset();
		ResponseEntity<String> result = testRestTemplate.exchange(
				"http://localhost:" + this.port + "/app/strip", HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		// Prefix not stripped to it goes to /local/strip
		assertEquals("Gotten strip!", result.getBody());
	}

}

// Don't use @SpringBootApplication because we don't want to component scan
@Configuration
@EnableAutoConfiguration
@RestController
@EnableZuulProxy
class ContextPathZuulProxyApplication {

	@RequestMapping(value = "/local/{id}", method = RequestMethod.GET)
	public String get(@PathVariable String id) {
		return "Gotten " + id + "!";
	}

}
