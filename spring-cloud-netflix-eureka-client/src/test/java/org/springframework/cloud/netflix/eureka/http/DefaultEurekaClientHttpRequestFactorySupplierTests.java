/*
 * Copyright 2013-present the original author or authors.
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

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.cloud.netflix.eureka.TimeoutProperties;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultEurekaClientHttpRequestFactorySupplier}.
 *
 * <p>
 * These specifically guard against regressing gh-4275: an earlier fix (gh-4258) made this
 * class a Spring {@code DisposableBean}, which raced with
 * {@code CloudEurekaClient#shutdown()} during context shutdown and broke
 * unregister-on-shutdown. That fix was reverted, and this supplier is now intentionally
 * stateless (gh-4569): it never caches or closes an HTTP client itself. Lifecycle
 * management of the shared client belongs to the owning
 * {@link RestClientTransportClientFactory}, which is already scoped to a single Eureka
 * client - see {@link RestClientTransportClientFactoryShutdownTests}.
 */
class DefaultEurekaClientHttpRequestFactorySupplierTests {

	private final DefaultEurekaClientHttpRequestFactorySupplier supplier = new DefaultEurekaClientHttpRequestFactorySupplier(
			new TimeoutProperties(), Collections.emptySet());

	@Test
	void shouldNotBeADisposableBean() {
		// Guard against reintroducing gh-4275: this class must not be destroyed via an
		// independent Spring bean-destroy callback.
		assertThat(supplier).isNotInstanceOf(DisposableBean.class);
	}

	@Test
	void getShouldReturnANonNullRequestFactory() {
		ClientHttpRequestFactory requestFactory = supplier.get(null, null);
		assertThat(requestFactory).isNotNull();
	}

	@Test
	void getShouldBuildAFreshHttpClientOnEveryCall() {
		// The supplier is stateless - caching and lifecycle management belong to the
		// caller (RestClientTransportClientFactory), so every call must return an
		// independent client rather than a shared one.
		ClientHttpRequestFactory first = supplier.get(null, null);
		ClientHttpRequestFactory second = supplier.get(null, null);

		Object firstHttpClient = ((HttpComponentsClientHttpRequestFactory) first).getHttpClient();
		Object secondHttpClient = ((HttpComponentsClientHttpRequestFactory) second).getHttpClient();

		assertThat(firstHttpClient).isNotSameAs(secondHttpClient);
	}

}
