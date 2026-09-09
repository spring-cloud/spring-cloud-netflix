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

import org.springframework.cloud.configuration.TlsProperties;
import org.springframework.cloud.netflix.eureka.TimeoutProperties;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests that {@link RestClientTransportClientFactory} owns and deterministically closes
 * the shared HTTP client it lazily builds via
 * {@link EurekaClientHttpRequestFactorySupplier#get}, rather than delegating that
 * lifecycle to a potentially shared supplier instance (gh-4569).
 *
 * <p>
 * Since each {@code RestClientTransportClientFactory} is already scoped to a single
 * Eureka client, caching the client here means
 * {@link RestClientTransportClientFactory#shutdown()} only ever closes a client owned
 * exclusively by this factory - there is no shared-state hazard between a refreshed
 * client or a second, independently-created Eureka client, as there would be if the
 * client were cached in the supplier itself.
 */
class RestClientTransportClientFactoryShutdownTests {

	private final DefaultEurekaClientHttpRequestFactorySupplier supplier = new DefaultEurekaClientHttpRequestFactorySupplier(
			new TimeoutProperties(), Collections.emptySet());

	private final RestClientTransportClientFactory factory = new RestClientTransportClientFactory(new TlsProperties(),
			supplier);

	@Test
	void shutdownBeforeAnyNewClientCallShouldNotThrow() {
		// shutdown() before newClient() (e.g. context shut down before any request was
		// ever made) must not throw.
		assertThatCode(factory::shutdown).doesNotThrowAnyException();
	}

	@Test
	void shutdownShouldBeIdempotent() {
		factory.newClient(endpoint());
		factory.shutdown();
		// Idempotent - shutdown paths may call shutdown() more than once.
		assertThatCode(factory::shutdown).doesNotThrowAnyException();
	}

	@Test
	void repeatedNewClientCallsShouldReuseTheSameCachedRequestFactory() {
		// newClient() delegates to the same cached request factory on every call, rather
		// than asking the supplier for a fresh one each time.
		factory.newClient(endpoint());
		Object firstHttpClient = cachedHttpClient(factory);

		factory.newClient(endpoint());
		Object secondHttpClient = cachedHttpClient(factory);

		assertThat(firstHttpClient).isSameAs(secondHttpClient);
	}

	private static com.netflix.discovery.shared.resolver.EurekaEndpoint endpoint() {
		com.netflix.discovery.shared.resolver.EurekaEndpoint endpoint = mock(
				com.netflix.discovery.shared.resolver.EurekaEndpoint.class);
		when(endpoint.getServiceUrl()).thenReturn("http://localhost:8761/eureka/");
		return endpoint;
	}

	private static Object cachedHttpClient(RestClientTransportClientFactory factory) {
		Object requestFactory = org.springframework.test.util.ReflectionTestUtils.getField(factory,
				"cachedRequestFactory");
		return ((HttpComponentsClientHttpRequestFactory) requestFactory).getHttpClient();
	}

}
