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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ErrorAttributes;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandFactory;
import org.springframework.cloud.netflix.zuul.filters.route.ZuulFallbackProvider;
import org.springframework.cloud.netflix.zuul.filters.route.apache.HttpClientRibbonCommandFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.Assert.assertEquals;

/**
 * @author Ryan Baxter
 */
public abstract class RibbonCommandFallbackTests {

	@LocalServerPort
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
	public void defaultFallback() {
		String uri = "/another/twolevel/slow";
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + uri, HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("default fallback", result.getBody());
	}

	// Don't use @SpringBootApplication because we don't want to component scan
	@Configuration
	@EnableAutoConfiguration
	@RestController
	@EnableZuulProxy
	@RibbonClients({
			@RibbonClient(name = "simple", configuration = ZuulProxyTestBase.SimpleRibbonClientConfiguration.class),
			@RibbonClient(name = "another", configuration = ZuulProxyTestBase.AnotherRibbonClientConfiguration.class)})
	public static class TestConfig extends ZuulProxyTestBase.AbstractZuulProxyApplication {

		@Autowired(required = false)
		private Set<ZuulFallbackProvider> zuulFallbackProviders = Collections.emptySet();


		@Bean
		public RibbonCommandFactory<?> ribbonCommandFactory(
				final SpringClientFactory clientFactory) {
			return new HttpClientRibbonCommandFactory(clientFactory, new ZuulProperties(),
					zuulFallbackProviders);
		}

		@Bean
		public ZuulProxyTestBase.MyErrorController myErrorController(
				ErrorAttributes errorAttributes) {
			return new ZuulProxyTestBase.MyErrorController(errorAttributes);
		}

		@Bean
		public ZuulFallbackProvider defaultFallbackProvider() {
			return new DefaultFallbackProvider();
		}
	}

	public static class DefaultFallbackProvider implements ZuulFallbackProvider {

		@Override
		public String getRoute() {
			return "*";
		}

		@Override
		public ClientHttpResponse fallbackResponse() {
			return new ClientHttpResponse() {
				@Override
				public HttpStatus getStatusCode() throws IOException {
					return HttpStatus.OK;
				}

				@Override
				public int getRawStatusCode() throws IOException {
					return 200;
				}

				@Override
				public String getStatusText() throws IOException {
					return null;
				}

				@Override
				public void close() {

				}

				@Override
				public InputStream getBody() throws IOException {
					return new ByteArrayInputStream("default fallback".getBytes());
				}

				@Override
				public HttpHeaders getHeaders() {
					HttpHeaders headers = new HttpHeaders();
					headers.setContentType(MediaType.TEXT_HTML);
					return headers;
				}
			};
		}
	}
}
