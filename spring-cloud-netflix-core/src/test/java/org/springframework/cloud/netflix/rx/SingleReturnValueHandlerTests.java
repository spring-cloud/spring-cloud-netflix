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
 */

package org.springframework.cloud.netflix.rx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import rx.Single;

/**
 * Tests the {@link SingleReturnValueHandler} class.
 *
 * @author Spencer Gibb
 * @author Jakub Narloch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SingleReturnValueHandlerTests.Application.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class SingleReturnValueHandlerTests {

	@Value("${local.server.port}")
	private int port = 0;

	private TestRestTemplate restTemplate = new TestRestTemplate();

	@Configuration
	@EnableAutoConfiguration
	@RestController
	protected static class Application {

		// tag::rx_single[]
		@RequestMapping(method = RequestMethod.GET, value = "/single")
		public Single<String> single() {
			return Single.just("single value");
		}

		@RequestMapping(method = RequestMethod.GET, value = "/singleWithResponse")
		public ResponseEntity<Single<String>> singleWithResponse() {
			return new ResponseEntity<>(Single.just("single value"),
					HttpStatus.NOT_FOUND);
		}
		
		@RequestMapping(method = RequestMethod.GET, value = "/singleCreatedWithResponse")
		public Single<ResponseEntity<String>> singleOuterWithResponse() {
			return Single.just(new ResponseEntity<>("single value", HttpStatus.CREATED));
		}

		@RequestMapping(method = RequestMethod.GET, value = "/throw")
		public Single<Object> error() {
			return Single.error(new RuntimeException("Unexpected"));
		}
		// end::rx_single[]
	}

	@Test
	public void shouldRetrieveSingleValue() {

		// when
		ResponseEntity<String> response = restTemplate.getForEntity(path("/single"),
				String.class);

		// then
		assertNotNull(response);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("single value", response.getBody());
	}

	@Test
	public void shouldRetrieveSingleValueWithStatusCode() {

		// when
		ResponseEntity<String> response = restTemplate
				.getForEntity(path("/singleWithResponse"), String.class);

		// then
		assertNotNull(response);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertEquals("single value", response.getBody());
	}

	@Test
	public void shouldRetrieveErrorResponse() {

		// when
		ResponseEntity<Object> response = restTemplate.getForEntity(path("/throw"),
				Object.class);

		// then
		assertNotNull(response);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
	}
	
	@Test
	public void shouldRetrieveSingleValueWithCreatedCode() {

		// when
		ResponseEntity<String> response = restTemplate.getForEntity(path("/singleCreatedWithResponse"),
				String.class);

		// then
		assertNotNull(response);
		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertEquals("single value", response.getBody());
	}

	private String path(String context) {
		return String.format("http://localhost:%d%s", port, context);
	}
}