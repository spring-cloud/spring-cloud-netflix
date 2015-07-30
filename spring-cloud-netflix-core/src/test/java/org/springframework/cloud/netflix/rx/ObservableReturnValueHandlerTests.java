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

package org.springframework.cloud.netflix.rx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import rx.Observable;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ObservableReturnValueHandlerTests.Application.class)
@WebAppConfiguration
@IntegrationTest({ "server.port=0" })
@DirtiesContext
public class ObservableReturnValueHandlerTests {

	@Value("${local.server.port}")
	private int port = 0;

	@Configuration
	@EnableAutoConfiguration
	@RestController
	protected static class Application {
		@RequestMapping(method = RequestMethod.GET, value = "/")
		public Observable<String> hi() {
			return Observable.just("hello world");
		}
	}

	@Test
	public void observableReturns() {
		ResponseEntity<String> response = new TestRestTemplate().getForEntity("http://localhost:" + port, String.class);
		assertNotNull("response was null", response);
		assertEquals("response code was wrong", HttpStatus.OK, response.getStatusCode());
		assertEquals("response was wrong", "hello world", response.getBody());
	}
}
