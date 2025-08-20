/*
 * Copyright 2017-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.eureka.http;

import java.util.Optional;

import com.netflix.discovery.shared.resolver.DefaultEndpoint;
import com.netflix.discovery.shared.transport.EurekaHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings.Redirects;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Daniel Lavoie
 * @author Wonchul Heo
 */
class RestTemplateTransportClientFactoryTests {

	private RestTemplateTransportClientFactory transportClientFactory;

	@BeforeEach
	void setup() {
		transportClientFactory = new RestTemplateTransportClientFactory();
	}

	@Test
	void withoutUserInfo() {
		EurekaHttpClient eurekaHttpClient = transportClientFactory
			.newClient(new DefaultEndpoint("http://localhost:8761"));
		assertThat(eurekaHttpClient).isInstanceOf(RestTemplateEurekaHttpClient.class);
	}

	@Test
	void invalidUserInfo() {
		EurekaHttpClient eurekaHttpClient = transportClientFactory
			.newClient(new DefaultEndpoint("http://test@localhost:8761"));
		assertThat(eurekaHttpClient).isInstanceOf(RestTemplateEurekaHttpClient.class);
	}

	@Test
	void userInfo() {
		EurekaHttpClient eurekaHttpClient = transportClientFactory
			.newClient(new DefaultEndpoint("http://test:test@localhost:8761"));
		assertThat(eurekaHttpClient).isInstanceOf(RestTemplateEurekaHttpClient.class);
	}

	@Test
	void testRequestFactorySetWithRestTemplateBuilderSupplier() {
		// Gateway Server WebMVC sets Redirects.DONT_FOLLOW, gh-4423
		RestTemplateBuilder builder = new RestTemplateBuilder()
			.requestFactorySettings(new ClientHttpRequestFactorySettings(Redirects.DONT_FOLLOW, null, null, null));
		transportClientFactory = new RestTemplateTransportClientFactory(Optional.empty(), Optional.empty(),
				new DefaultEurekaClientHttpRequestFactorySupplier(), () -> builder);
		EurekaHttpClient eurekaHttpClient = transportClientFactory
			.newClient(new DefaultEndpoint("http://localhost:8761"));
		RestTemplateEurekaHttpClient restTemplateEurekaHttpClient = (RestTemplateEurekaHttpClient) eurekaHttpClient;
		RestTemplate restTemplate = restTemplateEurekaHttpClient.getRestTemplate();
		ClientHttpRequestFactory requestFactory = restTemplate.getRequestFactory();
		if (requestFactory instanceof InterceptingClientHttpRequestFactory interceptingClientHttpRequestFactory) {
			requestFactory = interceptingClientHttpRequestFactory.getDelegate();
		}
		assertThat(requestFactory).isInstanceOf(HttpComponentsClientHttpRequestFactory.class);
	}

	@AfterEach
	void shutdown() {
		transportClientFactory.shutdown();
	}

}
