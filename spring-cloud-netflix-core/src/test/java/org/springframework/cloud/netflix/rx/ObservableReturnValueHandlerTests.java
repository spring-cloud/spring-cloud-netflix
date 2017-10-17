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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import rx.Observable;
import rx.Single;
import rx.functions.Func1;

/**
 * Tests the demonstrate using {@link Observable} with {@link SingleReturnValueHandler} class.
 *
 * @author Spencer Gibb
 * @author Jakub Narloch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ObservableReturnValueHandlerTests.Application.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class ObservableReturnValueHandlerTests {

	@Value("${local.server.port}")
	private int port = 0;

	private TestRestTemplate restTemplate = new TestRestTemplate();

	@Configuration
	@EnableAutoConfiguration
	@RestController
	protected static class Application {

		// tag::rx_observable[]
		@RequestMapping(method = RequestMethod.GET, value = "/single")
		public Single<String> single() {
			return Observable.just("single value").toSingle();
		}

		@RequestMapping(method = RequestMethod.GET, value = "/multiple")
		public Single<List<String>> multiple() {
			return Observable.just("multiple", "values").toList().toSingle();
		}

		@RequestMapping(method = RequestMethod.GET, value = "/responseWithObservable")
		public ResponseEntity<Single<String>> responseWithObservable() {

			Observable<String> observable = Observable.just("single value");
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(APPLICATION_JSON_UTF8);
			return new ResponseEntity<>(observable.toSingle(), headers, HttpStatus.CREATED);
		}

		@RequestMapping(method = RequestMethod.GET, value = "/timeout")
		public Observable<String> timeout() {
			return Observable.timer(1, TimeUnit.MINUTES).map(new Func1<Long, String>() {
				@Override
				public String call(Long aLong) {
					return "single value";
				}
			});
		}
		// end::rx_observable[]

		@RequestMapping(method = RequestMethod.GET, value = "/throw")
		public Single<Object> error() {
			return Observable.error(new RuntimeException("Unexpected")).toSingle();
		}
	}

	@Test
	public void shouldRetrieveSingleValue() {

		// when
		ResponseEntity<String> response = restTemplate.getForEntity(path("/single"), String.class);

		// then
		assertNotNull(response);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("single value", response.getBody());
	}

	@Test
	public void shouldRetrieveMultipleValues() {

		// when
		ResponseEntity<List> response = restTemplate.getForEntity(path("/multiple"), List.class);

		// then
		assertNotNull(response);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(Arrays.asList("multiple", "values"), response.getBody());
	}

	@Test
	public void shouldRetrieveSingleValueWithStatusCodeAndCustomHeader() {

		// when
		ResponseEntity<String> response = restTemplate.getForEntity(path("/responseWithObservable"), String.class);

		// then
		assertNotNull(response);
		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertEquals(MediaType.APPLICATION_JSON_UTF8, response.getHeaders().getContentType());
		assertEquals("single value", response.getBody());
	}

	@Test
	public void shouldRetrieveErrorResponse() {

		// when
		ResponseEntity<Object> response = restTemplate.getForEntity(path("/throw"), Object.class);

		// then
		assertNotNull(response);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
	}

	@Test
	@Ignore("adds 30s to build")
	public void shouldTimeoutOnConnection() {

		// when
		ResponseEntity<Object> response = restTemplate.getForEntity(path("/timeout"), Object.class);

		// then
		assertNotNull(response);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
	}

	private String path(String context) {
		return String.format("http://localhost:%d%s", port, context);
	}
}