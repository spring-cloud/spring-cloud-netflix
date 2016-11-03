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

import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.Assert.assertEquals;

/**
 * @author Ryan Baxter
 */
public abstract class RibbonCommandFallbackTests {

	@Value("${local.server.port}")
	protected int port;

	@Test
	public void fallback() {
		String uri = "/simple/slow";
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + uri, HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("fallback", result.getBody());
	}

	@Test
	public void noFallback() {
		String uri = "/another/twolevel/slow";
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + uri, HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		System.out.println("no fallback body: " + result.getBody());
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
	}
}
