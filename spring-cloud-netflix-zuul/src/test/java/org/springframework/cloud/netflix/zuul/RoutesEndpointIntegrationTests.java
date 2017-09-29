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
 *
 */

package org.springframework.cloud.netflix.zuul;

import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Ryan Baxter
 * @author Gregor Zurowski
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT,
		value = {"zuul.routes.sslservice.url=https://localhost:8443", "management.security.enabled=false"})
@DirtiesContext
public class RoutesEndpointIntegrationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private SimpleZuulProxyApplication.RoutesRefreshListener refreshListener;

	@Test
	@Ignore // FIXME: 2.0.x
	public void getRoutesTest() {
		Map<String, String> routes = restTemplate.getForObject("/admin/routes", Map.class);
		assertEquals("https://localhost:8443", routes.get("/sslservice/**"));
	}

	@Test
	@Ignore // FIXME: 2.0.x
	public void postRoutesTest() {
		Map<String, String> routes = restTemplate.postForObject("/admin/routes", null, Map.class);
		assertEquals("https://localhost:8443", routes.get("/sslservice/**"));
		assertTrue(refreshListener.wasCalled());
	}

	@Test
	@Ignore // FIXME: 2.0.x
	public void getRouteDetailsTest() {
		ResponseEntity<Map<String, RoutesEndpoint.RouteDetails>> responseEntity = restTemplate.exchange(
				"/admin/routes?format=details", HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, RoutesEndpoint.RouteDetails>>() {
				});

		assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));

		RoutesEndpoint.RouteDetails details = responseEntity.getBody().get("/sslservice/**");
		assertThat(details.getPath(), is("/**"));
		assertThat(details.getFullPath(), is("/sslservice/**"));
		assertThat(details.getLocation(), is("https://localhost:8443"));
		assertThat(details.getPrefix(), is("/sslservice"));
		assertTrue(details.isPrefixStripped());
	}

	@Configuration
	@EnableAutoConfiguration
	@RestController
	@EnableZuulProxy
	static class SimpleZuulProxyApplication {
		@Component
		static class RoutesRefreshListener implements ApplicationListener<RoutesRefreshedEvent> {
			private boolean called = false;
			@Override
			public void onApplicationEvent(RoutesRefreshedEvent routesRefreshedEvent) {
				called = true;
			}

			public boolean wasCalled() {
				return called;
			}
		}
	}
}
