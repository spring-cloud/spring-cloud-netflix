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

package org.springframework.cloud.netflix.zuul.filters.route.okhttp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ErrorAttributes;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.netflix.ribbon.DefaultServerIntrospector;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.ribbon.okhttp.OkHttpLoadBalancingClient;
import org.springframework.cloud.netflix.ribbon.test.TestLoadBalancer;
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
import org.springframework.web.bind.annotation.RestController;
import com.netflix.client.DefaultLoadBalancerRetryHandler;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = OkHttpRibbonCommandIntegrationTests.TestConfig.class, webEnvironment = WebEnvironment.RANDOM_PORT, value = {
		"zuul.routes.other: /test/**=http://localhost:7777/local",
		"zuul.routes.another: /another/twolevel/**", "zuul.routes.simple: /simple/**" })
@DirtiesContext
public class OkHttpRibbonCommandIntegrationTests extends ZuulProxyTestBase {

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
	public void ribbonCommandFactoryOverridden() {
		assertTrue("ribbonCommandFactory not a OkHttpRibbonCommandFactory",
				this.ribbonCommandFactory instanceof OkHttpRibbonCommandFactory);
	}

	// Don't use @SpringBootApplication because we don't want to component scan
	@Configuration
	@EnableAutoConfiguration
	@RestController
	@EnableZuulProxy
	@RibbonClients({
			@RibbonClient(name = "simple", configuration = SimpleRibbonClientConfiguration.class),
			@RibbonClient(name = "another", configuration = AnotherRibbonClientConfiguration.class) })
	static class TestConfig extends ZuulProxyTestBase.AbstractZuulProxyApplication {

		@Autowired(required = false)
		private Set<ZuulFallbackProvider> zuulFallbackProviders = Collections.emptySet();

		@Bean
		public RibbonCommandFactory<?> ribbonCommandFactory(
				final SpringClientFactory clientFactory) {
			return new OkHttpRibbonCommandFactory(clientFactory, new ZuulProperties(),
					zuulFallbackProviders);
		}

		@Bean
		public MyErrorController myErrorController(ErrorAttributes errorAttributes) {
			return new MyErrorController(errorAttributes);
		}

		@Bean
		public IClientConfig config() {
			return new DefaultClientConfigImpl();
		}

		@Bean
		public OkHttpLoadBalancingClient okClient(IClientConfig config) {
			final OkHttpLoadBalancingClient client = new OkHttpLoadBalancingClient(config,
					new DefaultServerIntrospector());
			client.setLoadBalancer(new TestLoadBalancer<>());
			client.setRetryHandler(new DefaultLoadBalancerRetryHandler());
			return client;
		}
	}
}
