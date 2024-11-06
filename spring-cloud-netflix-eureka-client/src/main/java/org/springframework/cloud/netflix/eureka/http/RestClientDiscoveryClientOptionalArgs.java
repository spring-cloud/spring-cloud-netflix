/*
 * Copyright 2017-2024 the original author or authors.
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

import java.util.function.Supplier;

import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import jakarta.ws.rs.client.ClientRequestFilter;

import org.springframework.web.client.RestClient;

/**
 * {@link RestClient} implementation of DiscoveryClientOptionalArgs that supports
 * supplying {@link ClientRequestFilter}.
 *
 * @author Wonchul Heo
 * @author Olga Maciaszek-Sharma
 * @since 4.2.0
 */
public class RestClientDiscoveryClientOptionalArgs extends AbstractDiscoveryClientOptionalArgs<Void> {

	private final EurekaClientHttpRequestFactorySupplier eurekaClientHttpRequestFactorySupplier;

	private final Supplier<RestClient.Builder> restClientBuilderSupplier;

	public RestClientDiscoveryClientOptionalArgs(
			EurekaClientHttpRequestFactorySupplier eurekaClientHttpRequestFactorySupplier,
			Supplier<RestClient.Builder> restClientBuilderSupplier) {
		this.eurekaClientHttpRequestFactorySupplier = eurekaClientHttpRequestFactorySupplier;
		this.restClientBuilderSupplier = restClientBuilderSupplier;
	}

	EurekaClientHttpRequestFactorySupplier getEurekaClientHttpRequestFactorySupplier() {
		return eurekaClientHttpRequestFactorySupplier;
	}

	Supplier<RestClient.Builder> getRestClientBuilderSupplier() {
		return restClientBuilderSupplier;
	}

}
