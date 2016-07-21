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

package org.springframework.cloud.netflix.feign.invalid;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignAutoConfiguration;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static org.junit.Assert.assertNotNull;

/**
 * @author Dave Syer
 */
public class FeignClientValidationTests {

	@Rule
	public ExpectedException expected = ExpectedException.none();

	@Test
	public void testNameAndValue() {
		this.expected.expectMessage("only one is permitted");
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				NameAndValueConfiguration.class);
		assertNotNull(context.getBean(NameAndValueConfiguration.Client.class));
		context.close();
	}

	@Configuration
	@Import(FeignAutoConfiguration.class)
	@EnableFeignClients(clients = NameAndValueConfiguration.Client.class)
	protected static class NameAndValueConfiguration {

		@FeignClient(value = "foo", name = "bar")
		interface Client {
			@RequestMapping(method = RequestMethod.GET, value = "/")
			String get();
		}

	}

	@Test
	public void testServiceIdAndValue() {
		this.expected.expectMessage("only one is permitted");
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				NameAndValueConfiguration.class);
		assertNotNull(context.getBean(NameAndServiceIdConfiguration.Client.class));
		context.close();
	}

	@Configuration
	@Import(FeignAutoConfiguration.class)
	@EnableFeignClients(clients = NameAndServiceIdConfiguration.Client.class)
	protected static class NameAndServiceIdConfiguration {

		@FeignClient(serviceId = "foo", name = "bar")
		interface Client {
			@RequestMapping(method = RequestMethod.GET, value = "/")
			String get();
		}

	}

	@Test
	public void testNotLegalHostname() {
		this.expected.expectMessage("not legal hostname (foo_bar)");
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				BadHostnameConfiguration.class);
		assertNotNull(context.getBean(BadHostnameConfiguration.Client.class));
		context.close();
	}

	@Configuration
	@Import(FeignAutoConfiguration.class)
	@EnableFeignClients(clients = BadHostnameConfiguration.Client.class)
	protected static class BadHostnameConfiguration {

		@FeignClient("foo_bar")
		interface Client {
			@RequestMapping(method = RequestMethod.GET, value = "/")
			String get();
		}

	}

	@Test
	public void testMissingFallback() {
		this.expected.expectMessage("No fallback instance of type");
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				MissingFallbackConfiguration.class);
		assertNotNull(context.getBean(MissingFallbackConfiguration.Client.class));
		context.close();
	}

	@Configuration
	@Import(FeignAutoConfiguration.class)
	@EnableFeignClients(clients = MissingFallbackConfiguration.Client.class)
	protected static class MissingFallbackConfiguration {

		@FeignClient(name = "foobar", url = "http://localhost", fallback = ClientFallback.class)
		interface Client {
			@RequestMapping(method = RequestMethod.GET, value = "/")
			String get();
		}

		class ClientFallback implements Client {
			@Override
			public String get() {
				return null;
			}
		}

	}

	@Test
	public void testWrongFallbackType() {
		this.expected.expectMessage("Incompatible fallback instance");
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				WrongFallbackTypeConfiguration.class);
		assertNotNull(context.getBean(WrongFallbackTypeConfiguration.Client.class));
		context.close();
	}

	@Configuration
	@Import(FeignAutoConfiguration.class)
	@EnableFeignClients(clients = WrongFallbackTypeConfiguration.Client.class)
	protected static class WrongFallbackTypeConfiguration {

		@FeignClient(name = "foobar", url = "http://localhost", fallback = Dummy.class)
		interface Client {
			@RequestMapping(method = RequestMethod.GET, value = "/")
			String get();
		}

		@Bean
		Dummy dummy() {
			return new Dummy();
		}

		class Dummy {
		}

	}
}
