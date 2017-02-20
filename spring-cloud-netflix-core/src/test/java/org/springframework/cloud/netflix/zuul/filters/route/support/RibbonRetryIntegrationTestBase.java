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
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.zuul.context.RequestContext;

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
		String uri = "/resetError";
		new TestRestTemplate().exchange(
				"http://localhost:" + this.port + uri, HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
	}


	@Test
	public void retryable() {
		String uri = "/retryable/everyothererror";
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + uri, HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
	}

	@Test
	public void postRetryOK() {
		String uri = "/retryable/posteveryothererror";
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + uri, HttpMethod.POST,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
	}

	@Test
	public void getRetryable() {
		String uri = "/getretryable/everyothererror";
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + uri, HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
	}

	@Test
	public void postNotRetryable() {
		String uri = "/getretryable/posteveryothererror";
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + uri, HttpMethod.POST,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
	}

	@Test
	public void disbaleRetry() {
		String uri = "/disableretry/everyothererror";
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + uri, HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		LOG.info("Response Body: " + result.getBody());
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
	}

	@Test
	public void globalRetryDisabled() {
		String uri = "/globalretrydisabled/everyothererror";
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + uri, HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		LOG.info("Response Body: " + result.getBody());
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
	}

	// Don't use @SpringBootApplication because we don't want to component scan
	@Configuration
	@EnableAutoConfiguration
	@RestController
	@EnableZuulProxy
	@RibbonClients({
			@RibbonClient(name = "retryable", configuration = RibbonClientConfiguration.class),
			@RibbonClient(name = "disableretry", configuration = RibbonClientConfiguration.class),
			@RibbonClient(name = "globalretrydisabled", configuration = RibbonClientConfiguration.class),
			@RibbonClient(name = "getretryable", configuration = RibbonClientConfiguration.class)})
	public static class RetryableTestConfig {

		private boolean error = true;

		@RequestMapping("/resetError")
		public void resetError() {
			error = true;
		}

		@RequestMapping("/everyothererror")
		public ResponseEntity<String> timeout() {
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

	}

	@Configuration
	public static class RibbonClientConfiguration {

		@Value("${local.server.port}")
		private int port;

		@Bean
		public ServerList<Server> ribbonServerList() {
			return new StaticServerList<>(new Server("localhost", this.port));
		}

	}
}
