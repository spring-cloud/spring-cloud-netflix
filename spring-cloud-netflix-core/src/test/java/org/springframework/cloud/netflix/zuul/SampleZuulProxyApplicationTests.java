/*
 * Copyright 2013-2015 the original author or authors.
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

import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.BasicErrorController;
import org.springframework.boot.autoconfigure.web.ErrorAttributes;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.discovery.DiscoveryClientRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.route.RestClientRibbonCommand;
import org.springframework.cloud.netflix.zuul.filters.route.RestClientRibbonCommandFactory;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandContext;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.client.ClientException;
import com.netflix.client.http.HttpRequest;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.niws.client.http.RestClient;

import lombok.SneakyThrows;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SampleZuulProxyApplication.class)
@WebAppConfiguration
@IntegrationTest({ "server.port: 0",
		"zuul.routes.other: /test/**=http://localhost:7777/local",
		"zuul.routes.another: /another/twolevel/**", "zuul.routes.simple: /simple/**",
		"zuul.routes.badhost: /badhost/**", "zuul.ignoredHeaders: X-Header",
		"zuul.routes.rnd: /rnd/**", "rnd.ribbon.listOfServers: ${random.value}",
		"zuul.removeSemicolonContent: false" })
@DirtiesContext
public class SampleZuulProxyApplicationTests extends ZuulProxyTestBase {

	@Autowired
	RouteLocator routeLocator;

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
				this.ribbonCommandFactory instanceof SampleZuulProxyApplication.MyRibbonCommandFactory);
	}

	@Test
	public void routeLocatorOverridden() {
		assertTrue("routeLocator not a MyRouteLocator",
				this.routeLocator instanceof SampleZuulProxyApplication.MyRouteLocator);
	}

}

// Don't use @SpringBootApplication because we don't want to component scan
@Configuration
@EnableAutoConfiguration
@RestController
@EnableZuulProxy
@RibbonClients({
		@RibbonClient(name = "badhost", configuration = SampleZuulProxyApplication.BadHostRibbonClientConfiguration.class),
		@RibbonClient(name = "simple", configuration = SimpleRibbonClientConfiguration.class),
		@RibbonClient(name = "another", configuration = AnotherRibbonClientConfiguration.class) })
class SampleZuulProxyApplication extends ZuulProxyTestBase.AbstractZuulProxyApplication {

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
		ResponseEntity<String> result = new ResponseEntity<String>(
				request.getRequestURI(), headers, HttpStatus.OK);
		return result;
	}

	@RequestMapping("/query")
	public String addQuery(HttpServletRequest request, @RequestParam String foo) {
		return request.getRequestURI() + "?foo=" + foo;
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
		return new MyRibbonCommandFactory(clientFactory);
	}

	@Bean
	public RouteLocator routeLocator(DiscoveryClient discoveryClient, ZuulProperties zuulProperties) {
		return new MyRouteLocator("/", discoveryClient, zuulProperties);
	}

	@Bean
	public MyErrorController myErrorController(ErrorAttributes errorAttributes) {
		return new MyErrorController(errorAttributes);
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleZuulProxyApplication.class, args);
	}

	public static class MyRibbonCommandFactory extends RestClientRibbonCommandFactory {

		public MyRibbonCommandFactory(SpringClientFactory clientFactory) {
			super(clientFactory);
		}

		@Override
		@SuppressWarnings("deprecation")
		@SneakyThrows
		public RestClientRibbonCommand create(RibbonCommandContext context) {
			String uri = context.getUri();
			if (uri.startsWith("/throwexception/")) {
				String code = uri.replace("/throwexception/", "");
				RestClient restClient = getClientFactory()
						.getClient(context.getServiceId(), RestClient.class);
				return new MyCommand(Integer.parseInt(code), context.getServiceId(),
						restClient, getVerb(context.getVerb()), context.getUri(),
						context.getRetryable(), context.getHeaders(), context.getParams(),
						context.getRequestEntity());
			}
			return super.create(context);
		}
	}

	static class MyCommand extends RestClientRibbonCommand {

		private int errorCode;

		public MyCommand(int errorCode, String commandKey, RestClient restClient,
				HttpRequest.Verb verb, String uri, Boolean retryable,
				MultiValueMap<String, String> headers,
				MultiValueMap<String, String> params, InputStream requestEntity)
						throws URISyntaxException {
			super(commandKey, restClient, verb, uri, retryable, headers, params,
					requestEntity);
			this.errorCode = errorCode;
		}

		@Override
		protected ClientHttpResponse forward() throws Exception {
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
			return new StaticServerList<>(new Server(UUID.randomUUID().toString(), 4322));
		}

	}

	static class MyRouteLocator extends DiscoveryClientRouteLocator {

		public MyRouteLocator(String servletPath, DiscoveryClient discovery, ZuulProperties properties) {
			super(servletPath, discovery, properties);
		}
	}
}
