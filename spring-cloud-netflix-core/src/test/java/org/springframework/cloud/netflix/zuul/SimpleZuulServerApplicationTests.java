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

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SimpleZuulServerApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class SimpleZuulServerApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate testRestTemplate;

	@Autowired
	private RouteLocator routes;

	private String getRoute(String path) {
		return this.routes.getMatchingRoute(path).getLocation();
	}

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
	public void bindRoute() {
		assertNotNull(getRoute("/testing123/**"));
	}

	@Test
	public void getOnSelf() {
		ResponseEntity<String> result = testRestTemplate.exchange("/", HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Hello world", result.getBody());
	}

	@Test
	public void getOnSelfViaFilter() {
		ResponseEntity<String> result = testRestTemplate.exchange("/testing123/1",
				HttpMethod.GET, new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
	}

}

// Don't use @SpringBootApplication because we don't want to component scan
@Configuration
@EnableAutoConfiguration
@RestController
@EnableZuulServer
class SimpleZuulServerApplication {

	@RequestMapping("/local")
	public String local() {
		return "Hello local";
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
				return PRE_TYPE;
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
		SpringApplication.run(SimpleZuulServerApplication.class, args);
	}

}
