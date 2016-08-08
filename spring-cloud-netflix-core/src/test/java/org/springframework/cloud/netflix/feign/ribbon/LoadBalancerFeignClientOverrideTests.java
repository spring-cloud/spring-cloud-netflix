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

package org.springframework.cloud.netflix.feign.ribbon;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.cloud.netflix.feign.FeignContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;

import feign.Request;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = LoadBalancerFeignClientOverrideTests.TestConfiguration.class, webEnvironment = WebEnvironment.RANDOM_PORT, value = {
		"spring.application.name=loadBalancerFeignClientTests",
		"feign.httpclient.enabled=false", "feign.okhttp.enabled=false" })
@DirtiesContext
public class LoadBalancerFeignClientOverrideTests {

	@Autowired
	private FeignContext context;

	@Test
	public void overrideRequestOptions() {
		// specific ribbon 'bar' configuration via spring bean
		Request.Options barOptions = this.context.getInstance("bar",
				Request.Options.class);
		assertEquals(1, barOptions.connectTimeoutMillis());
		assertEquals(2, barOptions.readTimeoutMillis());
		assertOptions(barOptions, "bar", 1, 2);

		// specific ribbon 'foo' configuration via application.yml
		Request.Options fooOptions = this.context.getInstance("foo",
				Request.Options.class);
		assertEquals(LoadBalancerFeignClient.DEFAULT_OPTIONS, fooOptions);
		assertOptions(fooOptions, "foo", 7, 17);

		// generic ribbon default configuration
		Request.Options bazOptions = this.context.getInstance("baz",
				Request.Options.class);
		assertEquals(LoadBalancerFeignClient.DEFAULT_OPTIONS, bazOptions);
		assertOptions(bazOptions, "baz", 3001, 60001);
	}

	void assertOptions(Request.Options options, String name, int expectedConnect,
			int expectedRead) {
		LoadBalancerFeignClient client = this.context.getInstance(name,
				LoadBalancerFeignClient.class);
		IClientConfig config = client.getClientConfig(options, name);
		assertEquals("connect was wrong for " + name, expectedConnect,
				config.get(CommonClientConfigKey.ConnectTimeout, -1).intValue());
		assertEquals("read was wrong for " + name, expectedRead,
				config.get(CommonClientConfigKey.ReadTimeout, -1).intValue());
	}

	@Configuration
	@EnableFeignClients(clients = { FooClient.class, BarClient.class, BazClient.class })
	@EnableAutoConfiguration
	protected static class TestConfiguration {
	}

	@FeignClient(value = "foo", configuration = FooConfiguration.class)
	interface FooClient {
		@RequestMapping("/")
		String get();

	}

	public static class FooConfiguration {
	}

	@FeignClient(value = "bar", configuration = BarConfiguration.class)
	interface BarClient {
		@RequestMapping("/")
		String get();
	}

	public static class BarConfiguration {
		@Bean
		public Request.Options feignRequestOptions() {
			return new Request.Options(1, 2);
		}
	}

	@FeignClient("baz")
	interface BazClient {
		@RequestMapping("/")
		String get();
	}
}
