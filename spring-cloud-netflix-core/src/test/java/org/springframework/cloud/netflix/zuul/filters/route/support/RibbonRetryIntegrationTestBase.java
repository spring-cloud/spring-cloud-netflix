/*
 *
 *  * Copyright 2013-2016 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.cloud.netflix.zuul.filters.route.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.netflix.zuul.context.RequestContext;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.Assert.assertEquals;

/**
 * @author Ryan Baxter
 */
public abstract class RibbonRetryIntegrationTestBase {

	private final Log LOG = LogFactory.getLog(RibbonRetryIntegrationTestBase.class);

	@Value("${local.server.port}")
	protected int port;

	@Before
	public void setup() {
		RequestContext.getCurrentContext().clear();
		String uri = "/reset";
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + uri, HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);

		assertEquals(HttpStatus.OK, result.getStatusCode());
	}


	@Test
	public void retryable() {
		String uri = "/retryable/everyothererror";
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + uri, HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertNumberOfRequests(2);
	}

	@Test
	public void postRetryOK() {
		String uri = "/retryable/posteveryothererror";
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + uri, HttpMethod.POST,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertNumberOfRequests(2);
	}

	@Test
	public void getRetryable() {
		String uri = "/getretryable/everyothererror";
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + uri, HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertNumberOfRequests(2);
	}

	@Test
	public void postNotRetryable() {
		String uri = "/getretryable/posteveryothererror";
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + uri, HttpMethod.POST,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
		assertNumberOfRequests(1);
	}

	@Test
	public void disbaleRetry() {
		String uri = "/disableretry/everyothererror";
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + uri, HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		LOG.info("Response Body: " + result.getBody());
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
		assertNumberOfRequests(1);
	}

	@Test
	public void globalRetryDisabled() {
		String uri = "/globalretrydisabled/everyothererror";
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + uri, HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		LOG.info("Response Body: " + result.getBody());
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
		assertNumberOfRequests(1);
	}

	@Test
	public void stopRetryAfterHystixTimeout() throws InterruptedException {
		String url = "http://localhost:" + this.port + "/stopretry/failbytimeout";
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				url, HttpMethod.GET, new HttpEntity<>((Void) null), String.class
		);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());

		// Wait 1 second before asserting. If retry operation was not aborted, there should be more
		// requests than expected
		Thread.sleep(1000L);

		// Ribbon timeout is 100ms. Hystrix timeout is 350ms. We should see 4 requests.
		assertNumberOfRequests(4);
	}

	private void assertNumberOfRequests(int expected) {
		String url;
		url = "http://localhost:" + this.port + "/numberofrequests";
		ResponseEntity<Integer> actual = new TestRestTemplate().exchange(
				url, HttpMethod.GET, new HttpEntity<>((Void) null), Integer.class
		);
		assertEquals(expected, actual.getBody().intValue());
	}

	// Don't use @SpringBootApplication because we don't want to component scan
	@Configuration
	@EnableAutoConfiguration
	@RestController
	@EnableZuulProxy
	public static class RetryableTestConfig {

		private volatile boolean error = true;
		private volatile int numberOfRequests = 0;

		@RequestMapping("/reset")
		public void reset() {
			error = true;
			numberOfRequests = 0;
		}

		@RequestMapping("/everyothererror")
		public ResponseEntity<String> timeout() {
			numberOfRequests++;
			boolean shouldError = error;
			error = !error;
			try {
				if(shouldError) {
					Thread.sleep(80000);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}


			return new ResponseEntity<String>("no error", HttpStatus.OK);
		}

		@RequestMapping(path = "/posteveryothererror", method = RequestMethod.POST)
		public ResponseEntity<String> postTimeout() {
			return timeout();
		}

		@RequestMapping("/numberofrequests")
		public ResponseEntity<Integer> numberOfRequests() {
			return new ResponseEntity<>(numberOfRequests, HttpStatus.OK);
		}

		@RequestMapping("/failbytimeout")
		public ResponseEntity<String> failbytimeout() {
			numberOfRequests++;
			try {
				Thread.sleep(80000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return new ResponseEntity<>("ok", HttpStatus.OK);
		}
	}
}
