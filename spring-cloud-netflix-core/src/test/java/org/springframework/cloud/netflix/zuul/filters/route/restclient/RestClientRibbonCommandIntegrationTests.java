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

package org.springframework.cloud.netflix.zuul.filters.route.restclient;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ErrorAttributes;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.discovery.DiscoveryClientRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.route.RestClientRibbonCommand;
import org.springframework.cloud.netflix.zuul.filters.route.RestClientRibbonCommandFactory;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandContext;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandFactory;
import org.springframework.cloud.netflix.zuul.filters.route.ZuulFallbackProvider;
import org.springframework.cloud.netflix.zuul.filters.route.support.NoEncodingFormHttpMessageConverter;
import org.springframework.cloud.netflix.zuul.filters.route.support.ZuulProxyTestBase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.client.ClientException;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.niws.client.http.RestClient;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = RestClientRibbonCommandIntegrationTests.TestConfig.class, webEnvironment = WebEnvironment.RANDOM_PORT, value = {
		"zuul.routes.other: /test/**=http://localhost:7777/local",
		"zuul.routes.another: /another/twolevel/**", "zuul.routes.simple: /simple/**",
		"zuul.routes.badhost: /badhost/**", "zuul.ignoredHeaders: X-Header",
		"zuul.routes.rnd: /rnd/**", "rnd.ribbon.listOfServers: ${random.value}",
		"zuul.removeSemicolonContent: false", "ribbon.restclient.enabled=true"})
@DirtiesContext
public class RestClientRibbonCommandIntegrationTests extends ZuulProxyTestBase {

	@Autowired
	DiscoveryClientRouteLocator routeLocator;

	@Override
	protected boolean supportsPatch() {
		return false;
	}

	@Override
	protected boolean supportsDeleteWithBody() {
		return false;
	}

	@Test
	public void simpleHostRouteWithTrailingSlash() {
		this.routes.addRoute("/self/**", "http://localhost:" + this.port + "/");
		this.endpoint.reset();
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/self/trailing-slash", HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("/trailing-slash", result.getBody());
		assertFalse(this.myErrorController.wasControllerUsed());
	}

	@Test
	public void simpleHostRouteWithNonExistentUrl() {
		this.routes.addRoute("/self/**", "http://localhost:" + this.port + "/");
		this.endpoint.reset();
		String uri = "/self/nonExistentUrl";
		this.myErrorController.setUriToMatch(uri);
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + uri, HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
		assertFalse(this.myErrorController.wasControllerUsed());
	}

	@Test
	public void simpleHostRouteIgnoredHeader() {
		this.routes.addRoute("/self/**", "http://localhost:" + this.port + "/");
		this.endpoint.reset();
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/self/add-header", HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertNull(result.getHeaders().get("X-Header"));
	}

	@Test
	public void simpleHostRouteDefaultIgnoredHeader() {
		this.routes.addRoute("/self/**", "http://localhost:" + this.port + "/");
		this.endpoint.reset();
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/self/add-header", HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("[testclient:0]",
				result.getHeaders().get("X-Application-Context").toString());
	}

	@Test
	public void simpleHostRouteWithQuery() {
		this.routes.addRoute("/self/**", "http://localhost:" + this.port + "/");
		this.endpoint.reset();
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/self/query?foo=bar", HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("/query?foo=bar", result.getBody());
	}

	@Test
	public void simpleHostRouteWithMatrix() {
		this.routes.addRoute("/self/**", "http://localhost:" + this.port + "/");
		this.endpoint.reset();
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/self/matrix/my;q=2;p=1/more;x=2",
				HttpMethod.GET, new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("my=1-2;more=2", result.getBody());
	}

	@Test
	public void simpleHostRouteWithEncodedQuery() {
		this.routes.addRoute("/self/**", "http://localhost:" + this.port + "/");
		this.endpoint.reset();
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/self/query?foo={foo}", HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class, "weird#chars");
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("/query?foo=weird#chars", result.getBody());
	}

	@Test
	public void simpleHostRouteWithColonParamNames() {
		this.routes.addRoute("/self/**", "http://localhost:" + this.port + "/");
		this.endpoint.reset();
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/self/colonquery?foo:bar={foobar0}&foobar={foobar1}", HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class, "baz", "bam");
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("/colonquery?foo:bar=baz&foobar=bam", result.getBody());
	}

	@Test
	public void simpleHostRouteWithContentType() {
		this.routes.addRoute("/self/**", "http://localhost:" + this.port + "/");
		this.endpoint.reset();
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/self/content-type", HttpMethod.POST,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("<NONE>", result.getBody());
	}

	@Test
	public void ribbonCommandForbidden() {
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/simple/throwexception/403",
				HttpMethod.GET, new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
	}

	@Test
	public void ribbonCommandServiceUnavailable() {
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/simple/throwexception/503",
				HttpMethod.GET, new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, result.getStatusCode());
	}

	@Test
	public void ribbonCommandBadHost() {
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/badhost/1", HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
		// JSON response
		assertThat(result.getBody(), containsString("\"status\":500"));
	}

	@Test
	public void ribbonCommandRandomHostFromConfig() {
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/rnd/1", HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
		// JSON response
		assertThat(result.getBody(), containsString("\"status\":500"));
	}

	@Test
	public void ribbonCommandFactoryOverridden() {
		assertTrue("ribbonCommandFactory not a MyRibbonCommandFactory",
				this.ribbonCommandFactory instanceof TestConfig.MyRibbonCommandFactory);
	}

	@Override
	@SuppressWarnings("deprecation")
	@Test
	public void javascriptEncodedFormParams() {
		TestRestTemplate testRestTemplate = new TestRestTemplate();
		ArrayList<HttpMessageConverter<?>> converters = new ArrayList<>();
		converters.addAll(Arrays.asList(new StringHttpMessageConverter(),
				new NoEncodingFormHttpMessageConverter()));
		testRestTemplate.getRestTemplate().setMessageConverters(converters);

		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("foo", "(bar)");
		ResponseEntity<String> result = testRestTemplate.postForEntity(
				"http://localhost:" + this.port + "/simple/local", map, String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Posted [(bar)] and Content-Length was: -1!", result.getBody());
	}

	@Test
	public void routeLocatorOverridden() {
		assertTrue("routeLocator not a MyRouteLocator",
				this.routeLocator instanceof TestConfig.MyRouteLocator);
	}

	// Don't use @SpringBootApplication because we don't want to component scan
	@Configuration
	@EnableAutoConfiguration
	@RestController
	@EnableZuulProxy
	@RibbonClients({
			@RibbonClient(name = "badhost", configuration = TestConfig.BadHostRibbonClientConfiguration.class),
			@RibbonClient(name = "simple", configuration = ZuulProxyTestBase.SimpleRibbonClientConfiguration.class),
			@RibbonClient(name = "another", configuration = ZuulProxyTestBase.AnotherRibbonClientConfiguration.class) })
	static class TestConfig extends ZuulProxyTestBase.AbstractZuulProxyApplication {

		@Autowired(required = false)
		private Set<ZuulFallbackProvider> fallbackProviders = Collections.emptySet();

		@RequestMapping("/trailing-slash")
		public String trailingSlash(HttpServletRequest request) {
			return request.getRequestURI();
		}

		@RequestMapping("/content-type")
		public String contentType(HttpServletRequest request) {
			String header = request.getHeader("Content-Type");
			return header == null ? "<NONE>" : header;
		}

		@RequestMapping("/add-header")
		public ResponseEntity<String> addHeader(HttpServletRequest request) {
			HttpHeaders headers = new HttpHeaders();
			headers.set("X-Header", "FOO");
			ResponseEntity<String> result = new ResponseEntity<>(
					request.getRequestURI(), headers, HttpStatus.OK);
			return result;
		}

		@RequestMapping("/query")
		public String query(HttpServletRequest request, @RequestParam String foo) {
			return request.getRequestURI() + "?foo=" + foo;
		}

		@RequestMapping("/colonquery")
		public String colonQuery(HttpServletRequest request, @RequestParam(name = "foo:bar") String foobar0, @RequestParam(name = "foobar") String foobar1) {
			return request.getRequestURI() + "?foo:bar=" + foobar0 + "&foobar=" + foobar1;
		}

		@RequestMapping("/matrix/{name}/{another}")
		public String matrix(@PathVariable("name") String name,
				@MatrixVariable(value = "p", pathVar = "name") int p,
				@MatrixVariable(value = "q", pathVar = "name") int q,
				@PathVariable("another") String another,
				@MatrixVariable(value = "x", pathVar = "another") int x) {
			return name + "=" + p + "-" + q + ";" + another + "=" + x;
		}

		@Bean
		public RibbonCommandFactory<?> ribbonCommandFactory(
				SpringClientFactory clientFactory) {
			return new MyRibbonCommandFactory(clientFactory, fallbackProviders);
		}

		@Bean
		public DiscoveryClientRouteLocator discoveryRouteLocator(
				DiscoveryClient discoveryClient,
				ZuulProperties zuulProperties) {
			return new MyRouteLocator("/", discoveryClient, zuulProperties);
		}

		@Bean
		public MyErrorController myErrorController(ErrorAttributes errorAttributes) {
			return new MyErrorController(errorAttributes);
		}

		public static void main(String[] args) {
			SpringApplication.run(TestConfig.class, args);
		}

		public static class MyRibbonCommandFactory
				extends RestClientRibbonCommandFactory {

			private SpringClientFactory clientFactory;

			public MyRibbonCommandFactory(SpringClientFactory clientFactory,
										  Set<ZuulFallbackProvider> fallbackProviders) {
				super(clientFactory, new ZuulProperties(), fallbackProviders);
				this.clientFactory = clientFactory;
			}

			@Override
			@SuppressWarnings("deprecation")
			public RestClientRibbonCommand create(RibbonCommandContext context) {
				String uri = context.getUri();
				if (uri.startsWith("/throwexception/")) {
					String code = uri.replace("/throwexception/", "");
					RestClient restClient = clientFactory
							.getClient(context.getServiceId(), RestClient.class);
					return new MyCommand(Integer.parseInt(code), context.getServiceId(),
							restClient, context);
				}
				return super.create(context);
			}
		}

		static class MyCommand extends RestClientRibbonCommand {

			private int errorCode;

			public MyCommand(int errorCode, String commandKey, RestClient restClient,
					RibbonCommandContext context) {
				super(commandKey, restClient, context, new ZuulProperties());
				this.errorCode = errorCode;
			}

			@Override
			protected ClientHttpResponse run() throws Exception {
				if (this.errorCode == 503) {
					throw new ClientException(ClientException.ErrorType.SERVER_THROTTLED);
				}
				return new MockClientHttpResponse((byte[]) null,
						HttpStatus.valueOf(this.errorCode));
			}
		}

		// Load balancer with fixed server list for "simple" pointing to bad host
		@Configuration
		static class BadHostRibbonClientConfiguration {
			@Bean
			public ServerList<Server> ribbonServerList() {
				return new StaticServerList<>(
						new Server(UUID.randomUUID().toString(), 4322));
			}

		}

		//This is needed to allow semicolon separators used in matrix variables
		@Configuration
		static class CustomHttpFirewallConfig implements WebSecurityConfigurer<WebSecurity> {
			@Override
			public void init(WebSecurity webSecurity) throws Exception {}

			@Override
			public void configure(WebSecurity builder) throws Exception {
				StrictHttpFirewall firewall = new StrictHttpFirewall();
				firewall.setAllowSemicolon(true);
				builder.httpFirewall(firewall);
			}
		}

		static class MyRouteLocator extends DiscoveryClientRouteLocator {

			public MyRouteLocator(String servletPath, DiscoveryClient discovery,
					ZuulProperties properties) {
				super(servletPath, discovery, properties);
			}
		}
	}
}
