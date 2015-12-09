/*
 *  Copyright 2013-2015 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.springframework.cloud.netflix.zuul;

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
import org.springframework.cloud.netflix.zuul.filters.ProxyRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.pre.ratelimit.RateLimitConfiguration;
import org.springframework.cloud.netflix.zuul.filters.pre.ratelimit.RateLimitFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import static org.junit.Assert.*;
/**
 * @author Vinicius Carvalho
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@SpringApplicationConfiguration(classes = RateLimitZuulApplication.class)
@IntegrationTest({ "server.port: 0",

		"zuul.ratelimit.enabled: true"
})
@DirtiesContext
public class RateLimitZuulProxyApplicationTests {
	@Value("${local.server.port}")
	private int port;

	@Autowired
	private ProxyRouteLocator routes;

	@Autowired
	private RoutesEndpoint endpoint;

	@Test
	public void getUnauthenticated() {
		routes.addRoute("/self/**","http://localhost:" + this.port + "/local");
		this.endpoint.reset();
		ResponseEntity<String> response = new TestRestTemplate().getForEntity("http://localhost:" + port + "/self/1",String.class);
		assertNotNull(response.getHeaders().get(RateLimitFilter.Headers.LIMIT));
		assertNotNull(response.getHeaders().get(RateLimitFilter.Headers.REMAINING));
		assertNotNull(response.getHeaders().get(RateLimitFilter.Headers.RESET));
	}
}

@Configuration
@EnableAutoConfiguration
@RestController
@EnableZuulProxy
@ComponentScan(basePackageClasses = RateLimitConfiguration.class)
class RateLimitZuulApplication {

	@RequestMapping(value = "/local/{id}", method = RequestMethod.GET)
	public String get(@PathVariable String id) {
		return "Gotten " + id + "!";
	}

	public static void main(String[] args) {
		SpringApplication.run(RateLimitZuulApplication.class);
	}
}
