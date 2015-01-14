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
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import com.netflix.zuul.ZuulFilter;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SampleZuulProxyApplication.class)
@WebAppConfiguration
@IntegrationTest({ "server.port: 0",
		"zuul.routes.other: /test/**=http://localhost:7777/local",
		"zuul.routes.another: /another/twolevel/**", "zuul.routes.simple: /simple/**" })
@DirtiesContext
public class SampleZuulProxyApplicationTests {

	@Value("${local.server.port}")
	private int port;

	@Autowired
	private ProxyRouteLocator routes;

	@Autowired
	private RoutesEndpoint endpoint;

	@Test
	public void bindRouteUsingPhysicalRoute() {
		assertEquals("http://localhost:7777/local",
				this.routes.getRoutes().get("/test/**"));
	}

	@Test
	public void bindRouteUsingOnlyPath() {
		assertEquals("simple", this.routes.getRoutes().get("/simple/**"));
	}

	@Test
	public void getOnSelfViaRibbonRoutingFilter() {
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/simple/local/1", HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Gotten!", result.getBody());
	}

	@Test
	public void deleteOnSelfViaSimpleHostRoutingFilter() {
		this.routes.addRoute("/self/**", "http://localhost:" + this.port + "/local");
		this.endpoint.reset();
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/self/1", HttpMethod.DELETE,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Deleted!", result.getBody());
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
		assertEquals("Gotten!", result.getBody());
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
class SampleZuulProxyApplication {

	@RequestMapping("/testing123")
	public String testing123() {
		throw new RuntimeException("myerror");
	}

	@RequestMapping("/local")
	public String local() {
		return "Hello local";
	}

	@RequestMapping(value = "/local/{id}", method = RequestMethod.DELETE)
	public String delete() {
		return "Deleted!";
	}

	@RequestMapping(value = "/local/{id}", method = RequestMethod.GET)
	public String get() {
		return "Gotten!";
	}

	@RequestMapping("/")
	public String home() {
		return "Hello world";
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
				return null;
			}

			@Override
			public int filterOrder() {
				return 0;
			}
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleZuulProxyApplication.class, args);
	}

}

// Load balancer with fixed server list for "simple" pointing to localhost
@Configuration
class SimpleRibbonClientConfiguration {

	@Bean
	public ILoadBalancer ribbonLoadBalancer(EurekaInstanceConfig instance) {
		BaseLoadBalancer balancer = new BaseLoadBalancer();
		balancer.setServersList(Arrays.asList(new Server("localhost", instance
				.getNonSecurePort())));
		return balancer;
	}

}

@Configuration
class AnotherRibbonClientConfiguration {

	@Bean
	public ILoadBalancer ribbonLoadBalancer(EurekaInstanceConfig instance) {
		BaseLoadBalancer balancer = new BaseLoadBalancer();
		balancer.setServersList(Arrays.asList(new Server("localhost", instance
				.getNonSecurePort())));
		return balancer;
	}

}
