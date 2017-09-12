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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.discovery.DiscoveryClientRouteMatcher;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RestController;

/**
 * This test class is to make sure that advance routing configuration is loaded properly from
 * application.yml
 * 
 * @author Mustansar Anwar
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = AdvancedZuulRoutingTests.AdvancedZuulProxyApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT, value = {
		"zuul.forceOriginalQueryStringEncoding: true",
		"zuul.route-matcher.advanced: true" })
@DirtiesContext
public class AdvancedZuulRoutingTests {

	@Autowired
	private DiscoveryClientRouteMatcher routes;

	@Test
	public void httpPutCallShouldGoToDefaultUrlCompact() {
		MockHttpServletRequest request = new MockHttpServletRequest("PUT",
				"/advanceroutescompact/user");
		Route route = routes.getMatchingRoute(request);

		assertEquals(route.getLocation(), "http://user-default-service");
	}

	@Test
	public void getCallShouldGoToItsOwnSpecifiedUrlCompact() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET",
				"/advanceroutescompact/user");
		Route route = routes.getMatchingRoute(request);

		assertEquals(route.getLocation(), "http://user-get-service");
	}

	@Test
	public void postCallShouldGoToItsOwnSpecifiedUrlCompact() {
		MockHttpServletRequest request = new MockHttpServletRequest("POST",
				"/advanceroutescompact/user");
		Route route = routes.getMatchingRoute(request);

		assertEquals(route.getLocation(), "http://user-post-service");
	}
	
	@Test
	public void httpPutCallShouldGoToDefaultUrlExpanded() {
		MockHttpServletRequest request = new MockHttpServletRequest("PUT",
				"/advanceroutesexpanded/user");
		Route route = routes.getMatchingRoute(request);

		assertEquals(route.getLocation(), "http://user-default-service");
	}

	@Test
	public void getCallShouldGoToItsOwnSpecifiedUrlExpanded() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET",
				"/advanceroutesexpanded/user");
		Route route = routes.getMatchingRoute(request);

		assertEquals(route.getLocation(), "http://user-get-service");
	}

	@Test
	public void postCallShouldGoToItsOwnSpecifiedUrlExpanded() {
		MockHttpServletRequest request = new MockHttpServletRequest("POST",
				"/advanceroutesexpanded/user");
		Route route = routes.getMatchingRoute(request);

		assertEquals(route.getLocation(), "http://user-post-service");
	}
	
	

	// Don't use @SpringBootApplication because we don't want to component scan
	@Configuration
	@EnableAutoConfiguration
	@RestController
	@EnableZuulProxy
	static class AdvancedZuulProxyApplication {

	}
}
