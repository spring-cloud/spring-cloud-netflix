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
 * unregister-on-shutdown. That fix was reverted; this class must continue to be closed
 * only via {@link EurekaClientHttpRequestFactorySupplier#close()}, invoked synchronously
 * by {@code TransportClientFactory#shutdown()} - never via an independent Spring
 * bean-destroy callback.
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
	void shouldReuseSameHttpClientAcrossMultipleGetCalls() {
		ClientHttpRequestFactory first = supplier.get(null, null);
		ClientHttpRequestFactory second = supplier.get(null, null);

		Object firstHttpClient = ((HttpComponentsClientHttpRequestFactory) first).getHttpClient();
		Object secondHttpClient = ((HttpComponentsClientHttpRequestFactory) second).getHttpClient();

		assertThat(firstHttpClient).isSameAs(secondHttpClient);
	}

	@Test
	void closeShouldBeSafeToCallWithoutPriorGet() {
		// close() before get() (e.g. context shut down before any request was ever
		// made) must not throw.
		supplier.close();
	}

	@Test
	void closeShouldBeSafeToCallTwice() {
		supplier.get(null, null);
		supplier.close();
		// Idempotent - shutdown paths may call close() more than once.
		supplier.close();
	}

	@Test
	void getAfterCloseShouldStillReturnARequestFactory() {
		supplier.get(null, null);
		supplier.close();

		// A get() call racing just after shutdown must not throw; the returned factory
		// wraps a closed client and will fail on actual use, which is expected during
		// shutdown, but construction itself must remain safe.
		ClientHttpRequestFactory afterClose = supplier.get(null, null);
		assertThat(afterClose).isNotNull();
	}

}
