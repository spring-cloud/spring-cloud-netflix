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

package org.springframework.cloud.netflix.feign.hystrix;

import java.util.Base64;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.cloud.netflix.feign.hystrix.app.CustomCommandHookImpl;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

/**
 * Tests that a secured web service returning values using a feign client properly access
 * the security context from a hystrix command.
 * @author Daniel Lavoie
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = FeignHystrixSecurityApplication.class)
@WebIntegrationTest(randomPort = true, value = {
		"" })
public class FeignHystrixSecurityTests {
	@Autowired
	private CustomCommandHook customCommandHook;

	@Value("${local.server.port}")
	private String serverPort;

	@Value("${security.user.username}")
	private String username;

	@Value("${security.user.password}")
	private String password;

	@Test
	public void testFeignHystrixSecurity() {
		HttpHeaders headers = FeignHystrixSecurityTests.createBasicAuthHeader(username,
				password);

		String usernameResult = new RestTemplate().exchange(
				"http://localhost:" + serverPort + "/proxy-username",
				HttpMethod.GET, new HttpEntity<Void>(headers), String.class).getBody();

		Assert.assertTrue("Username should have been intercepted by feign interceptor.",
				username.equals(usernameResult));

		Assert.assertTrue("Custom hook should have been called.",
				((CustomCommandHookImpl)customCommandHook).isHookCalled());
	}

	public static HttpHeaders createBasicAuthHeader(final String username,
			final String password) {
		return new HttpHeaders() {
			private static final long serialVersionUID = 1766341693637204893L;

			{
				String auth = username + ":" + password;
				byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
				String authHeader = "Basic " + new String(encodedAuth);
				this.set("Authorization", authHeader);
			}
		};
	}
}
