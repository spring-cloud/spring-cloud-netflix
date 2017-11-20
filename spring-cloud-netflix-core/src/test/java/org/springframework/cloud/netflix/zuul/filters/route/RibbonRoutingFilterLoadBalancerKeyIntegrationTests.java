/*
 * Copyright 2013-2017 the original author or authors.
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

package org.springframework.cloud.netflix.zuul.filters.route;

import com.netflix.loadbalancer.AvailabilityFilteringRule;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.LOAD_BALANCER_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;

/**
 * @author Yongsung Yoon
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = CanaryTestZuulProxyApplication.class,
				webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
				value = { "zuul.routes.simple.path: /simple/**" })
@DirtiesContext
public class RibbonRoutingFilterLoadBalancerKeyIntegrationTests {

	@Autowired
	private TestRestTemplate testRestTemplate;

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
	public void invokeWithUserDefinedCanaryHeader() {
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Canary-Test", "true");

		ResponseEntity<String> result = testRestTemplate.exchange("/simple/hello", HttpMethod.GET,
				new HttpEntity<>(headers), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("canary", result.getBody());
	}

	@Test
	public void invokeWithoutUserDefinedCanaryHeader() {
		HttpHeaders headers = new HttpHeaders();
		ResponseEntity<String> result = testRestTemplate.exchange("/simple/hello", HttpMethod.GET,
				new HttpEntity<>(headers), String.class);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
	}
}

@Configuration
@EnableAutoConfiguration
@RestController
@EnableZuulProxy
@RibbonClient(name = "simple", configuration = CanaryTestRibbonClientConfiguration.class)
class CanaryTestZuulProxyApplication {

	@RequestMapping(value = "/hello", method = RequestMethod.GET)
	public String hello() {
		return "canary";
	}

	@Bean
	public ZuulFilter testCanarySupportPreFilter() {
		return new ZuulFilter() {
			@Override
			public Object run() {
				RequestContext context = RequestContext.getCurrentContext();
				if (checkIfCanaryRequest(context)) {
					context.set(LOAD_BALANCER_KEY, "canary"); // set loadBalancerKey for IRule
				}
				return null;
			}

			private boolean checkIfCanaryRequest(RequestContext context) {
				HttpServletRequest request = context.getRequest();
				String canaryHeader = request.getHeader("X-Canary-Test"); // user defined header

				if ((canaryHeader != null) && (canaryHeader.equalsIgnoreCase("true"))) {
					return true;
				}
				return false;
			}

			@Override
			public boolean shouldFilter() {
				return true;
			}

			@Override
			public String filterType() {
				return PRE_TYPE;
			}

			@Override
			public int filterOrder() {
				return 0;
			}
		};
	}
}

@Configuration
class CanaryTestRibbonClientConfiguration {

	@LocalServerPort
	private  int port;

	private static Server testCanaryInstance;

	@Bean
	public ServerList<Server> ribbonServerList() {
		return new StaticServerList<>(new Server("normal-routing-notexist-localhost", this.port));
	}

	@Bean
	public IRule canaryTestRule() {
		if (testCanaryInstance == null) {
			testCanaryInstance = new Server("localhost", port); // use test server as a canary instance
		}
		return new TestCanaryRule();
	}

	public static class TestCanaryRule extends AvailabilityFilteringRule {
		@Override
		public Server choose(Object key) {
			if ((key != null) && (key.equals("canary"))) {
				return testCanaryInstance;  // choose test canary server instead of normal servers.
			}
			return super.choose(key); // normal routing
		}
	}
}
