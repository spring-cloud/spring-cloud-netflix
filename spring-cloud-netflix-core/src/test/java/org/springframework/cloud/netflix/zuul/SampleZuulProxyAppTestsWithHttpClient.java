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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.discovery.DiscoveryClientRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandFactory;
import org.springframework.cloud.netflix.zuul.filters.route.apache.HttpClientRibbonCommandFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SampleHttpClientZuulProxyApplication.class)
@WebAppConfiguration
@IntegrationTest({ "server.port: 0",
		"zuul.routes.other: /test/**=http://localhost:7777/local",
		"zuul.routes.another: /another/twolevel/**", "zuul.routes.simple: /simple/**" })
@DirtiesContext
public class SampleZuulProxyAppTestsWithHttpClient {

	@Value("${local.server.port}")
	private int port;

	@Autowired
	private DiscoveryClientRouteLocator routes;

	@Autowired
	private RoutesEndpoint endpoint;

	@Autowired
	private RibbonCommandFactory<?> ribbonCommandFactory;

	private String getRoute(String path) {
		return this.routes.getRoutes().get(path);
	}

	@Test
	public void bindRouteUsingPhysicalRoute() {
		assertEquals("http://localhost:7777/local", getRoute("/test/**"));
	}

	@Test
	public void bindRouteUsingOnlyPath() {
		assertEquals("simple", getRoute("/simple/**"));
	}

	@Test
	public void getOnSelfViaRibbonRoutingFilter() {
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/simple/local/1", HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Gotten 1!", result.getBody());
	}

	@Test
	public void patchOnSelfViaRibbonRoutingFilter() {
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/simple/local/1", HttpMethod.PATCH,
				new HttpEntity<>("TestPatch"), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Patched 1!", result.getBody());
	}

	@Test
	public void postOnSelfViaRibbonRoutingFilter() {
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/simple/local/1", HttpMethod.POST,
				new HttpEntity<>("TestPost"), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Posted 1!", result.getBody());
	}

	@Test
	public void deleteOnSelfViaRibbonRoutingFilter() {
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/simple/local/1", HttpMethod.DELETE,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Deleted 1!", result.getBody());
	}

	@Test
	public void deleteOnSelfViaSimpleHostRoutingFilter() {
		this.routes.addRoute("/self/**", "http://localhost:" + this.port + "/local");
		this.endpoint.reset();

		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/self/1", HttpMethod.DELETE,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Deleted 1!", result.getBody());
	}

	@Test
	public void patchOnSelfViaSimpleHostRoutingFilter() {
		this.routes.addRoute("/self/**", "http://localhost:" + this.port + "/local");
		this.endpoint.reset();

		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/self/1", HttpMethod.PATCH,
				new HttpEntity<>("TestPatch"), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Patched 1!", result.getBody());
	}

	@Test
	public void stripPrefixFalseAppendsPath() {
		this.routes.addRoute(new ZuulProperties.ZuulRoute("strip", "/strip/**", "strip",
				"http://localhost:" + this.port + "/local", false, false));
		this.endpoint.reset();

		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/strip", HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());

		// Prefix not stripped to it goes to /local/strip
		assertEquals("Gotten strip!", result.getBody());
	}

	@Test
	public void testNotFound() {
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/myinvalidpath", HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
	}

	@Test
	public void getSecondLevel() {
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/another/twolevel/local/1",
				HttpMethod.GET, new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Gotten 1!", result.getBody());
	}

	@Test
	public void ribbonRouteWithSpace() {
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/simple/spa ce", HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Hello space", result.getBody());
	}

	@Test
	public void simpleHostRouteWithSpace() {
		this.routes.addRoute("/self/**", "http://localhost:" + this.port);
		this.endpoint.reset();

		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/self/spa ce", HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Hello space", result.getBody());
	}

	@Test
	public void simpleHostRouteWithOriginalQString() {
		this.routes.addRoute("/self/**", "http://localhost:" + this.port);
		this.endpoint.reset();

		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port
						+ "/self/qstring?original=value1&original=value2",
				HttpMethod.GET, new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Received {original=[value1, value2]}", result.getBody());
	}

	@Test
	public void simpleHostRouteWithOverriddenQString() {
		this.routes.addRoute("/self/**", "http://localhost:" + this.port);
		this.endpoint.reset();

		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port
						+ "/self/qstring?override=true&different=key",
				HttpMethod.GET, new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Received {key=[overridden]}", result.getBody());
	}

	@Test
	public void ribbonCommandFactoryOverridden() {
		assertTrue("ribbonCommandFactory not a MyRibbonCommandFactory",
				this.ribbonCommandFactory instanceof HttpClientRibbonCommandFactory);
	}

}

// Don't use @SpringBootApplication because we don't want to component scan
@Configuration
@EnableAutoConfiguration
@RestController
@EnableZuulProxy
@RibbonClients({
		@RibbonClient(name = "simple", configuration = SimpleRibbonClientConfiguration.class),
		@RibbonClient(name = "another", configuration = AnotherRibbonClientConfiguration.class) })
class SampleHttpClientZuulProxyApplication {

	public static void main(final String[] args) {
		SpringApplication.run(SampleZuulProxyApplication.class, args);
	}

	@RequestMapping("/testing123")
	public String testing123() {
		throw new RuntimeException("myerror");
	}

	@RequestMapping("/local")
	public String local() {
		return "Hello local";
	}

	@RequestMapping(value = "/local/{id}", method = RequestMethod.DELETE)
	public String delete(@PathVariable final String id) {
		return "Deleted " + id + "!";
	}

	@RequestMapping(value = "/local/{id}", method = RequestMethod.PATCH)
	public String patch(@PathVariable final String id, @RequestBody final String body) {
		return "Patched " + id + "!";
	}

	@RequestMapping(value = "/local/{id}", method = RequestMethod.GET)
	public String get(@PathVariable final String id) {
		return "Gotten " + id + "!";
	}

	@RequestMapping(value = "/local/{id}", method = RequestMethod.POST)
	public String post(@PathVariable final String id, @RequestBody final String body) {
		return "Posted " + id + "!";
	}

	@RequestMapping(value = "/qstring")
	public String qstring(@RequestParam final MultiValueMap<String, String> params) {
		return "Received " + params.toString();
	}

	@RequestMapping("/")
	public String home() {
		return "Hello world";
	}

	@RequestMapping("/spa ce")
	public String space() {
		return "Hello space";
	}

	@Bean
	public RibbonCommandFactory<?> ribbonCommandFactory(
			final SpringClientFactory clientFactory) {
		return new HttpClientRibbonCommandFactory(clientFactory);
	}

	@Bean
	public ZuulFilter sampleFilter() {
		return new ZuulFilter() {
			@Override
			public String filterType() {
				return "pre";
			}

			@Override
			public boolean shouldFilter() {
				return true;
			}

			@Override
			public Object run() {
				if (RequestContext.getCurrentContext().getRequest().getParameterMap()
						.containsKey("override")) {
					Map<String, List<String>> overridden = new HashMap<>();
					overridden.put("key", Arrays.asList("overridden"));
					RequestContext.getCurrentContext().setRequestQueryParams(overridden);
				}

				return null;
			}

			@Override
			public int filterOrder() {
				return 0;
			}
		};
	}

}
