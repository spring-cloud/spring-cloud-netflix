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

import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.zuul.exception.ZuulException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.cloud.netflix.zuul.filters.route.RestClientRibbonCommand;
import org.springframework.cloud.netflix.zuul.filters.route.RestClientRibbonCommandFactory;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandContext;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.SneakyThrows;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SampleZuulProxyApplication.class)
@WebAppConfiguration
@IntegrationTest({ "server.port: 0",
		"zuul.routes.other: /test/**=http://localhost:7777/local",
		"zuul.routes.another: /another/twolevel/**", "zuul.routes.simple: /simple/**",
		"zuul.routes.badhost: /badhost/**" })
@DirtiesContext
public class SampleZuulProxyApplicationTests extends ZuulProxyTestBase {

	@Test
	public void simpleHostRouteWithTrailingSlash() {
		this.routes.addRoute("/self/**", "http://localhost:" + this.port + "/");
		this.endpoint.reset();
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/self/trailing-slash", HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("/trailing-slash", result.getBody());
	}

	@Test
	public void ribbonCommandForbidden() {
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/simple/throwexception/403",
				HttpMethod.GET, new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
	}

	@Test
	public void ribbonCommandBadHost() {
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/badhost/1",
				HttpMethod.GET, new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
	}

	@Test
	public void ribbonCommandFactoryOverridden() {
		assertTrue(
				"ribbonCommandFactory not a MyRibbonCommandFactory",
				this.ribbonCommandFactory instanceof SampleZuulProxyApplication.MyRibbonCommandFactory);
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

	@RequestMapping(value = "/trailing-slash")
	public String trailingSlash(HttpServletRequest request) {
		return request.getRequestURI();
	}

	@Bean
	public RibbonCommandFactory<?> ribbonCommandFactory(SpringClientFactory clientFactory) {
		return new MyRibbonCommandFactory(clientFactory);
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleZuulProxyApplication.class, args);
	}

	public static class MyRibbonCommandFactory extends RestClientRibbonCommandFactory {

		public MyRibbonCommandFactory(SpringClientFactory clientFactory) {
			super(clientFactory);
		}

		@Override
		@SneakyThrows
		public RestClientRibbonCommand create(RibbonCommandContext context) {
			String uri = context.getUri();
			if (uri.startsWith("/throwexception/")) {
				String code = uri.replace("/throwexception/", "");
				throw new ZuulException(new RuntimeException(), Integer.parseInt(code),
						"test error");
			}
			return super.create(context);
		}
	}

	// Load balancer with fixed server list for "simple" pointing to localhost
	@Configuration
	static class BadHostRibbonClientConfiguration {
		@Bean
		public ServerList<Server> ribbonServerList() {
			return new StaticServerList<>(new Server(UUID.randomUUID().toString(), 4322));
		}

	}
}
