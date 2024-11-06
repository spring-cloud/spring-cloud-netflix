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

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

/**
 * @author Daniel Lavoie
 * @author Armin Krezovic
 * @deprecated {@link RestTemplate}-based implementation to be removed in favour of
 * {@link RestClient}-based implementation.
 */
@Deprecated
public class RestTemplateDiscoveryClientOptionalArgs extends AbstractDiscoveryClientOptionalArgs<Void> {

	protected final EurekaClientHttpRequestFactorySupplier eurekaClientHttpRequestFactorySupplier;

	protected final Supplier<RestTemplateBuilder> restTemplateBuilderSupplier;

	public RestTemplateDiscoveryClientOptionalArgs(
			EurekaClientHttpRequestFactorySupplier eurekaClientHttpRequestFactorySupplier) {
		this(eurekaClientHttpRequestFactorySupplier, RestTemplateBuilder::new);
	}

	public RestTemplateDiscoveryClientOptionalArgs(
			EurekaClientHttpRequestFactorySupplier eurekaClientHttpRequestFactorySupplier,
			Supplier<RestTemplateBuilder> restTemplateBuilderSupplier) {
		this.eurekaClientHttpRequestFactorySupplier = eurekaClientHttpRequestFactorySupplier;
		this.restTemplateBuilderSupplier = restTemplateBuilderSupplier;
	}

}
