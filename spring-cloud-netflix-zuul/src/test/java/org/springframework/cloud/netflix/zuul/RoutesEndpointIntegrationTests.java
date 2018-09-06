/*
 * Copyright 2013-2018 the original author or authors.
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

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.netflix.zuul.test.NoSecurityConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
		value = {"zuul.routes.sslservice.url=https://localhost:8443", "management.security.enabled=false", "management.endpoints.web.exposure.include=*"})
@DirtiesContext
public class RoutesEndpointIntegrationTests {
	private static final String BASE_PATH = new WebEndpointProperties().getBasePath();

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private SimpleZuulProxyApplication.RoutesRefreshListener refreshListener;

	@Test
	@SuppressWarnings("unchecked")
	public void getRoutesTest() {
		ResponseEntity<Map> entity = restTemplate.getForEntity(BASE_PATH + "/routes", Map.class);
		Assertions.assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		Map<String, String> routes = entity.getBody();
		assertEquals("https://localhost:8443", routes.get("/sslservice/**"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void postRoutesTest() {
		ResponseEntity<Map> entity = restTemplate.postForEntity(BASE_PATH + "/routes", null, Map.class);
		Assertions.assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		Map<String, String> routes = entity.getBody();
		assertEquals("https://localhost:8443", routes.get("/sslservice/**"));
		assertTrue(refreshListener.wasCalled());
	}

	@Test
	public void getRouteDetailsTest() {
		ResponseEntity<Map<String, RoutesEndpoint.RouteDetails>> responseEntity = restTemplate.exchange(
				BASE_PATH + "/routes/details", HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, RoutesEndpoint.RouteDetails>>() {
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
	@Import(NoSecurityConfiguration.class)
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
