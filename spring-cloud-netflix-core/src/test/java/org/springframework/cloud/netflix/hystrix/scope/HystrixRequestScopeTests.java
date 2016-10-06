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
package org.springframework.cloud.netflix.hystrix.scope;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

/**
 * Tests that a Hystrix command works properly using Spring beans with request scope.
 *
 * Created by ctasso on 05/10/2016.
 *
 * @author Claudio Tasso
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
@SpringBootTest(classes = HystrixRequestScopeTests.MyApplication.class)
public class HystrixRequestScopeTests {

	@Autowired
	private MyService myService;

	@Test
	public void testRequestScopedBean() {

		Assert.assertEquals("ciao", myService.hello());
	}

	@SpringBootApplication
	@Configuration
	@EnableCircuitBreaker
	public static class MyApplication {

		@Bean
		public MyService myService() {
			return new MyService();
		}

		@Bean
		@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
		public RequestService requestService() {
			return new RequestService();
		}

	}

	public static class MyService {

		@Autowired
		private RequestService requestService;

		@HystrixCommand
		public String hello() {
			return requestService.execute();
		}
	}

	public static class RequestService {
		public String execute() {
			return "ciao";
		}
	}
}
