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

package org.springframework.cloud.netflix.rx;

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

import rx.Completable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Tests the {@link CompletableReturnValueHandler} class.
 *
 * @author Kyryl Sablin
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = CompletableReturnValueHandlerTest.Application.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class CompletableReturnValueHandlerTest {

	@Value("${local.server.port}")
	private int port = 0;

	private TestRestTemplate restTemplate = new TestRestTemplate();

	@Configuration
	@EnableAutoConfiguration
	@RestController
	protected static class Application {

		@RequestMapping(method = RequestMethod.GET, value = "/completable")
		public Completable completable() {
			return Completable.complete();
		}

		@RequestMapping(method = RequestMethod.GET, value = "/completableWithResponse")
		public ResponseEntity<Completable> completableWithResponse() {
			return new ResponseEntity<>(Completable.complete(),
					HttpStatus.NOT_FOUND);
		}

		@RequestMapping(method = RequestMethod.GET, value = "/throw")
		public Completable error() {
			return Completable.error(new RuntimeException("Unexpected"));
		}
	}

	@Test
	public void shouldRetrieveResponseWithoutBody() {

		// when
		ResponseEntity<Void> response = restTemplate.getForEntity(path("/completable"),
				Void.class);

		// then
		assertNotNull(response);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNull(response.getBody());
	}

	@Test
	public void shouldRetrieveWithoutBodyButWithStatusCode() {

		// when
		ResponseEntity<Void> response = restTemplate
				.getForEntity(path("/completableWithResponse"), Void.class);

		// then
		assertNotNull(response);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
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

	private String path(String context) {
		return String.format("http://localhost:%d%s", port, context);
	}
}
