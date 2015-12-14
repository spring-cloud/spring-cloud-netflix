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

import static org.junit.Assert.*;

import feign.Contract;
import feign.Feign;
import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.auth.BasicAuthRequestInterceptor;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.hystrix.HystrixFeign;
import feign.slf4j.Slf4jLogger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
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

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = FeignClientOverrideDefaultsTests.TestConfiguration.class)
@DirtiesContext
public class FeignClientOverrideDefaultsTests {

	@Autowired
	private FeignClientFactory factory;

	@Test
	public void overrideDecoder() {
		Decoder.Default.class.cast(factory.getInstance("foo", Decoder.class));
		ResponseEntityDecoder.class.cast(factory.getInstance("bar", Decoder.class));
	}

	@Test
	public void overrideEncoder() {
		Encoder.Default.class.cast(factory.getInstance("foo", Encoder.class));
		SpringEncoder.class.cast(factory.getInstance("bar", Encoder.class));
	}

	@Test
	public void overrideLogger() {
		Logger.JavaLogger.class.cast(factory.getInstance("foo", Logger.class));
		Slf4jLogger.class.cast(factory.getInstance("bar", Logger.class));
	}

	@Test
	public void overrideContract() {
		Contract.Default.class.cast(factory.getInstance("foo", Contract.class));
		SpringMvcContract.class.cast(factory.getInstance("bar", Contract.class));
	}

	@Test
	public void overrideLoggerLevel() {
		assertNull(factory.getInstance("foo", Logger.Level.class));
		assertEquals(Logger.Level.HEADERS, factory.getInstance("bar", Logger.Level.class));
	}

	@Test
	public void overrideRetryer() {
		assertNull(factory.getInstance("foo", Retryer.class));
		Retryer.Default.class.cast(factory.getInstance("bar", Retryer.class));
	}

	@Test
	public void overrideErrorDecoder() {
		assertNull(factory.getInstance("foo", ErrorDecoder.class));
		ErrorDecoder.Default.class.cast(factory.getInstance("bar", ErrorDecoder.class));
	}

	@Test
	public void overrideBuilder() {
		Feign.Builder.class.cast(factory.getInstance("foo", Feign.Builder.class));
		HystrixFeign.Builder.class.cast(factory.getInstance("bar", Feign.Builder.class));
	}

	@Test
	public void overrideRequestOptions() {
		assertNull(factory.getInstance("foo", Request.Options.class));
		Request.Options options = factory.getInstance("bar", Request.Options.class);
		assertEquals(1, options.connectTimeoutMillis());
		assertEquals(1, options.readTimeoutMillis());
	}

	@Test
	public void addRequestInterceptor() {
		assertNull(factory.getInstance("foo", RequestInterceptor.class));
		BasicAuthRequestInterceptor.class.cast(factory.getInstance("bar", RequestInterceptor.class));
	}

	@Configuration
	@EnableFeignClients(clients = {FooClient.class, BarClient.class})
	@Import({ PropertyPlaceholderAutoConfiguration.class,
			ArchaiusAutoConfiguration.class, FeignAutoConfiguration.class})
	protected static class TestConfiguration {
	}

	@FeignClient(value = "foo", configuration = FooConfiguration.class)
	interface FooClient{
		@RequestMapping("/")
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
			return Feign.builder();
		}
	}

	@FeignClient(value = "bar", configuration = BarConfiguration.class)
	interface BarClient{
		@RequestMapping("/")
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
