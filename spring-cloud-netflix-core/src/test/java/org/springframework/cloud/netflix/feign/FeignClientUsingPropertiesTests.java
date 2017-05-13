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

package org.springframework.cloud.netflix.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.RetryableException;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.*;

/**
 * @author Eko Kurniawan Khannedy
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = FeignClientUsingPropertiesTests.Application.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:feign-properties.properties")
@DirtiesContext
public class FeignClientUsingPropertiesTests {

	@Autowired
	FeignContext context;

	@Autowired
	private ApplicationContext applicationContext;

	@Value("${local.server.port}")
	private int port = 0;

	private FeignClientFactoryBean fooFactoryBean;

	private FeignClientFactoryBean barFactoryBean;

	public FeignClientUsingPropertiesTests() {
		fooFactoryBean = new FeignClientFactoryBean();
		fooFactoryBean.setName("foo");
		fooFactoryBean.setType(FeignClientFactoryBean.class);

		barFactoryBean = new FeignClientFactoryBean();
		barFactoryBean.setName("bar");
		barFactoryBean.setType(FeignClientFactoryBean.class);
	}

	public FooClient fooClient() {
		fooFactoryBean.setApplicationContext(applicationContext);
		return fooFactoryBean.feign(context).target(FooClient.class, "http://localhost:" + this.port);
	}

	public BarClient barClient() {
		barFactoryBean.setApplicationContext(applicationContext);
		return barFactoryBean.feign(context).target(BarClient.class, "http://localhost:" + this.port);
	}

	@Test
	public void testFoo() {
		String response = fooClient().foo();
		assertNotNull("OK", response);
	}

	@Test(expected = RetryableException.class)
	public void testBar() {
		barClient().bar();
		fail("it should timeout");
	}

	protected interface FooClient {

		@RequestMapping(method = RequestMethod.GET, value = "/foo")
		String foo();
	}

	protected interface BarClient {

		@RequestMapping(method = RequestMethod.GET, value = "/bar")
		String bar();
	}

	@Configuration
	@EnableAutoConfiguration
	@RestController
	protected static class Application {

		@RequestMapping(method = RequestMethod.GET, value = "/foo")
		public String foo(HttpServletRequest request) throws IllegalAccessException {
			if ("Foo".equals(request.getHeader("Foo")) &&
					"Bar".equals(request.getHeader("Bar"))) {
				return "OK";
			} else {
				throw new IllegalAccessException("It should has Foo and Bar header");
			}
		}

		@RequestMapping(method = RequestMethod.GET, value = "/bar")
		public String bar() throws InterruptedException {
			Thread.sleep(2000L);
			return "OK";
		}

	}

	public static class FooRequestInterceptor implements RequestInterceptor {
		@Override
		public void apply(RequestTemplate template) {
			template.header("Foo", "Foo");
		}
	}

	public static class BarRequestInterceptor implements RequestInterceptor {
		@Override
		public void apply(RequestTemplate template) {
			template.header("Bar", "Bar");
		}
	}

	public static class NoRetryer implements Retryer {

		@Override
		public void continueOrPropagate(RetryableException e) {
			throw e;
		}

		@Override
		public Retryer clone() {
			return this;
		}
	}

	public static class DefaultErrorDecoder extends ErrorDecoder.Default {
	}

}
