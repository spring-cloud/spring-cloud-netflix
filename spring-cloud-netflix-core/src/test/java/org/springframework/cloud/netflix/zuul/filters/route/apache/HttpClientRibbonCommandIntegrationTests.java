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

package org.springframework.cloud.netflix.zuul.filters.route.apache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpHeaders.SET_COOKIE;

import java.util.Collections;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ErrorAttributes;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.netflix.ribbon.DefaultServerIntrospector;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.cloud.netflix.ribbon.apache.RibbonLoadBalancingHttpClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandFactory;
import org.springframework.cloud.netflix.zuul.filters.route.ZuulFallbackProvider;
import org.springframework.cloud.netflix.zuul.filters.route.support.ZuulProxyTestBase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.WebUtils;

import com.netflix.client.RetryHandler;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = HttpClientRibbonCommandIntegrationTests.TestConfig.class, webEnvironment = WebEnvironment.RANDOM_PORT, value = {
		"zuul.routes.other: /test/**=http://localhost:7777/local",
		"zuul.routes.another: /another/twolevel/**", "zuul.routes.simple: /simple/**",
		"zuul.routes.singleton: /singleton/**",
		"zuul.routes.singleton.sensitiveHeaders: " })
@DirtiesContext
public class HttpClientRibbonCommandIntegrationTests extends ZuulProxyTestBase {

	@Before
	public void init() {
		super.setTestRequestcontext();
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
	public void ribbonLoadBalancingHttpClientCookiePolicy() {
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/simple/downstream_cookie",
				HttpMethod.POST, new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Cookie 434354454!", result.getBody());
		assertNull(result.getHeaders().getFirst(SET_COOKIE));

		// if new instance of RibbonLoadBalancingHttpClient is getting created every time
		// and HttpClient is not reused then there are no concerns for the shared cookie
		// storage
		// but since https://github.com/spring-cloud/spring-cloud-netflix/issues/1150 is
		// on the way a
		result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/singleton/downstream_cookie",
				HttpMethod.POST, new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Cookie 434354454!", result.getBody());
		assertEquals("jsessionid=434354454", result.getHeaders().getFirst(SET_COOKIE));

		result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/singleton/downstream_cookie",
				HttpMethod.GET, new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Cookie null!", result.getBody());
	}

	@Test
	public void ribbonCommandFactoryOverridden() {
		assertTrue("ribbonCommandFactory not a HttpClientRibbonCommandFactory",
				this.ribbonCommandFactory instanceof HttpClientRibbonCommandFactory);
	}

	// Don't use @SpringBootApplication because we don't want to component scan
	@Configuration
	@EnableAutoConfiguration
	@RestController
	@EnableZuulProxy
	@RibbonClients({
			@RibbonClient(name = "simple", configuration = ZuulProxyTestBase.SimpleRibbonClientConfiguration.class),
			@RibbonClient(name = "another", configuration = ZuulProxyTestBase.AnotherRibbonClientConfiguration.class),
			@RibbonClient(name = "singleton", configuration = SingletonRibbonClientConfiguration.class) })
	static class TestConfig extends ZuulProxyTestBase.AbstractZuulProxyApplication {

		@Autowired(required = false)
		private Set<ZuulFallbackProvider> zuulFallbackProviders = Collections.emptySet();

		@RequestMapping(value = "/local/{id}", method = RequestMethod.PATCH)
		public String patch(@PathVariable final String id,
				@RequestBody final String body) {
			return "Patched " + id + "!";
		}

		@RequestMapping(value = "/downstream_cookie", method = RequestMethod.POST)
		public String setDownstreamCookie(HttpServletResponse response) {
			response.addCookie(new Cookie("jsessionid", "434354454"));
			return "Cookie 434354454!";
		}

		@RequestMapping(value = "/downstream_cookie", method = RequestMethod.GET)
		public String readDownstreamCookie(HttpServletRequest request) {
			final Cookie cookie = WebUtils.getCookie(request, "jsessionid");
			return "Cookie " + cookie + "!";
		}

		@Bean
		public RibbonCommandFactory<?> ribbonCommandFactory(
				final SpringClientFactory clientFactory) {
			return new HttpClientRibbonCommandFactory(clientFactory, new ZuulProperties(),
					zuulFallbackProviders);
		}

		@Bean
		public ZuulProxyTestBase.MyErrorController myErrorController(
				ErrorAttributes errorAttributes) {
			return new ZuulProxyTestBase.MyErrorController(errorAttributes);
		}
	}

	// Load balancer with fixed server list and defined ribbon rest client
	@Configuration
	public static class SingletonRibbonClientConfiguration {

		@Value("${local.server.port}")
		private int port;

		@Bean
		public ServerList<Server> ribbonServerList() {
			return new StaticServerList<>(new Server("localhost", this.port));
		}

		@Bean
		public RibbonLoadBalancingHttpClient ribbonClient(IClientConfig config,
				ILoadBalancer loadBalancer, RetryHandler retryHandler) {
			final RibbonLoadBalancingHttpClient client = new RibbonLoadBalancingHttpClient(config,
					new DefaultServerIntrospector());
			client.setLoadBalancer(loadBalancer);
			client.setRetryHandler(retryHandler);
			return client;
		}

	}
}
