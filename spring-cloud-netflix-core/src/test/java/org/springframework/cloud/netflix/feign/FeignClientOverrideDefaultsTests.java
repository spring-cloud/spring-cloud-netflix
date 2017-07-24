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

package org.springframework.cloud.netflix.feign;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration;
import org.springframework.cloud.netflix.feign.support.ResponseEntityDecoder;
import org.springframework.cloud.netflix.feign.support.SpringEncoder;
import org.springframework.cloud.netflix.feign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import feign.Contract;
import feign.Feign;
import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.RequestLine;
import feign.RequestTemplate;
import feign.Retryer;
import feign.auth.BasicAuthRequestInterceptor;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.hystrix.HystrixFeign;
import feign.slf4j.Slf4jLogger;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = FeignClientOverrideDefaultsTests.TestConfiguration.class)
@DirtiesContext
public class FeignClientOverrideDefaultsTests {

	@Autowired
	private FeignContext context;

	@Autowired
	private FooClient foo;

	@Autowired
	private BarClient bar;

	@Test
	public void clientsAvailable() {
		assertNotNull(this.foo);
		assertNotNull(this.bar);
	}

	@Test
	public void overrideDecoder() {
		Decoder.Default.class.cast(this.context.getInstance("foo", Decoder.class));
		ResponseEntityDecoder.class.cast(this.context.getInstance("bar", Decoder.class));
	}

	@Test
	public void overrideEncoder() {
		Encoder.Default.class.cast(this.context.getInstance("foo", Encoder.class));
		SpringEncoder.class.cast(this.context.getInstance("bar", Encoder.class));
	}

	@Test
	public void overrideLogger() {
		Logger.JavaLogger.class.cast(this.context.getInstance("foo", Logger.class));
		Slf4jLogger.class.cast(this.context.getInstance("bar", Logger.class));
	}

	@Test
	public void overrideContract() {
		Contract.Default.class.cast(this.context.getInstance("foo", Contract.class));
		SpringMvcContract.class.cast(this.context.getInstance("bar", Contract.class));
	}

	@Test
	public void overrideLoggerLevel() {
		assertNull(this.context.getInstance("foo", Logger.Level.class));
		assertEquals(Logger.Level.HEADERS,
				this.context.getInstance("bar", Logger.Level.class));
	}

	@Test
	public void overrideRetryer() {
		assertEquals(Retryer.NEVER_RETRY, this.context.getInstance("foo", Retryer.class));
		Retryer.Default.class.cast(this.context.getInstance("bar", Retryer.class));
	}

	@Test
	public void overrideErrorDecoder() {
		assertNull(this.context.getInstance("foo", ErrorDecoder.class));
		ErrorDecoder.Default.class
				.cast(this.context.getInstance("bar", ErrorDecoder.class));
	}

	@Test
	public void overrideBuilder() {
		HystrixFeign.Builder.class.cast(this.context.getInstance("foo", Feign.Builder.class));
		Feign.Builder.class
				.cast(this.context.getInstance("bar", Feign.Builder.class));
	}

	@Test
	public void overrideRequestOptions() {
		assertNull(this.context.getInstance("foo", Request.Options.class));
		Request.Options options = this.context.getInstance("bar", Request.Options.class);
		assertEquals(1, options.connectTimeoutMillis());
		assertEquals(1, options.readTimeoutMillis());
	}

	@Test
	public void addRequestInterceptor() {
		assertEquals(1,
				this.context.getInstances("foo", RequestInterceptor.class).size());
		assertEquals(2,
				this.context.getInstances("bar", RequestInterceptor.class).size());
	}

	@Configuration
	@EnableFeignClients(clients = { FooClient.class, BarClient.class })
	@Import({ PropertyPlaceholderAutoConfiguration.class, ArchaiusAutoConfiguration.class,
			FeignAutoConfiguration.class })
	protected static class TestConfiguration {
		@Bean
		RequestInterceptor defaultRequestInterceptor() {
			return new RequestInterceptor() {
				@Override
				public void apply(RequestTemplate template) {
				}
			};
		}
	}

	@FeignClient(name = "foo", url = "http://foo", configuration = FooConfiguration.class)
	interface FooClient {
		@RequestLine("GET /")
		String get();

	}

	public static class FooConfiguration {
		@Bean
		public Decoder feignDecoder() {
			return new Decoder.Default();
		}

		@Bean
		public Encoder feignEncoder() {
			return new Encoder.Default();
		}

		@Bean
		public Logger feignLogger() {
			return new Logger.JavaLogger();
		}

		@Bean
		public Contract feignContract() {
			return new Contract.Default();
		}

		@Bean
		public Feign.Builder feignBuilder() {
			return HystrixFeign.builder();
		}
	}

	@FeignClient(name = "bar", url = "http://bar", configuration = BarConfiguration.class)
	interface BarClient {
		@RequestMapping(value = "/", method = RequestMethod.GET)
		String get();
	}

	public static class BarConfiguration {
		@Bean
		Logger.Level feignLevel() {
			return Logger.Level.HEADERS;
		}

		@Bean
		Retryer feignRetryer() {
			return new Retryer.Default();
		}

		@Bean
		ErrorDecoder feignErrorDecoder() {
			return new ErrorDecoder.Default();
		}

		@Bean
		Request.Options feignRequestOptions() {
			return new Request.Options(1, 1);
		}

		@Bean
		RequestInterceptor feignRequestInterceptor() {
			return new BasicAuthRequestInterceptor("user", "pass");
		}
	}
}
