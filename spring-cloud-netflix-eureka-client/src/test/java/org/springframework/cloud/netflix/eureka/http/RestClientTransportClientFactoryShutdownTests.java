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

import org.junit.jupiter.api.Test;

import org.springframework.cloud.configuration.TlsProperties;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests that {@link RestClientTransportClientFactory#shutdown()} deterministically
 * delegates to {@link EurekaClientHttpRequestFactorySupplier#close()}, closing the shared
 * HTTP client/pool synchronously - after the caller (Netflix's {@code DiscoveryClient})
 * has already completed its final {@code unregister()} call, and not via a separate,
 * unordered Spring bean-destroy path (gh-4569).
 */
class RestClientTransportClientFactoryShutdownTests {

	@Test
	void shutdownShouldCloseTheHttpRequestFactorySupplier() {
		EurekaClientHttpRequestFactorySupplier supplier = mock(EurekaClientHttpRequestFactorySupplier.class);
		RestClientTransportClientFactory factory = new RestClientTransportClientFactory(new TlsProperties(), supplier);

		factory.shutdown();

		verify(supplier, times(1)).close();
	}

	@Test
	void shutdownShouldBeIdempotent() {
		EurekaClientHttpRequestFactorySupplier supplier = mock(EurekaClientHttpRequestFactorySupplier.class);
		RestClientTransportClientFactory factory = new RestClientTransportClientFactory(new TlsProperties(), supplier);

		factory.shutdown();
		factory.shutdown();

		verify(supplier, times(2)).close();
	}

}
