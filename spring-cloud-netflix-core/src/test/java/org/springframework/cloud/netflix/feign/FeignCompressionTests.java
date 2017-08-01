/*
 *
 *  * Copyright 2013-2017 the original author or authors.
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

package org.springframework.cloud.netflix.feign;

import feign.Client;
import feign.RequestInterceptor;
import feign.httpclient.ApacheHttpClient;

import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.ClassPathExclusions;
import org.springframework.cloud.FilteredClassPathRunner;
import org.springframework.cloud.commons.httpclient.HttpClientConfiguration;
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration;
import org.springframework.cloud.netflix.feign.encoding.FeignAcceptGzipEncodingAutoConfiguration;
import org.springframework.cloud.netflix.feign.encoding.FeignAcceptGzipEncodingInterceptor;
import org.springframework.cloud.netflix.feign.encoding.FeignContentGzipEncodingAutoConfiguration;
import org.springframework.cloud.netflix.feign.encoding.FeignContentGzipEncodingInterceptor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Ryan Baxter
 */
@RunWith(FilteredClassPathRunner.class)
@ClassPathExclusions({"ribbon-loadbalancer-{version:\\d.*}.jar"})
public class FeignCompressionTests {

	private ConfigurableApplicationContext context;

	@Before
	public void setUp() {
		context = new SpringApplicationBuilder().properties("feign.compression.response.enabled=true",
				"feign.compression.request.enabled=true", "feign.okhttp.enabled=false").sources(PropertyPlaceholderAutoConfiguration.class,
				ArchaiusAutoConfiguration.class, FeignAutoConfiguration.class, PlainConfig.class, FeignContentGzipEncodingAutoConfiguration.class,
				FeignAcceptGzipEncodingAutoConfiguration.class, HttpClientConfiguration.class).web(false).run();
	}

	@After
	public void tearDown() {
		if(context != null) {
			context.close();
		}
	}

	@Test
	public void testInterceptors() {
		FeignContext feignContext = context.getBean(FeignContext.class);
		Map<String, RequestInterceptor> interceptors = feignContext.getInstances("foo", RequestInterceptor.class);
		assertEquals(2, interceptors.size());
		assertTrue(FeignAcceptGzipEncodingInterceptor.class.isInstance(interceptors.get("feignAcceptGzipEncodingInterceptor")));
		assertTrue(FeignContentGzipEncodingInterceptor.class.isInstance(interceptors.get("feignContentGzipEncodingInterceptor")));
	}

	@Configuration
	protected static class PlainConfig {

		@Autowired
		private Client client;

		@Bean
		public ApacheHttpClient client() {
			/* We know our client is an AppacheHttpClient because we disabled the OK HTTP client.  FeignAcceptGzipEncodingAutoConfiguration
			 * won't load unless there is a bean of type ApacheHttpClient (not Client) in this test because the bean is not
			 * yet created and so the application context doesnt know that the Client bean is actually an instance of ApacheHttpClient,
			 * therefore FeignAcceptGzipEncodingAutoConfiguration will not be loaded.  We just create a bean here of type
			 * ApacheHttpClient so that the configuration will be loaded correctly.
			 */
			return (ApacheHttpClient)client;
		}
	}
}
