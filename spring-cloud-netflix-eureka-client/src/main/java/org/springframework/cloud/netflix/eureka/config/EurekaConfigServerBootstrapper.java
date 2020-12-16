/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.config;

import com.netflix.discovery.shared.transport.EurekaHttpClient;

import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.Bootstrapper;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServerInstanceProvider;
import org.springframework.cloud.configuration.TlsProperties;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.http.DefaultEurekaClientHttpRequestFactorySupplier;
import org.springframework.cloud.netflix.eureka.http.EurekaClientHttpRequestFactorySupplier;
import org.springframework.cloud.netflix.eureka.http.RestTemplateTransportClientFactory;
import org.springframework.util.ClassUtils;

public class EurekaConfigServerBootstrapper implements Bootstrapper {

	@Override
	public void intitialize(BootstrapRegistry registry) {
		if (!ClassUtils.isPresent("org.springframework.cloud.config.client.ConfigServerInstanceProvider", null)) {
			return;
		}
		registry.registerIfAbsent(EurekaClientConfigBean.class, context -> {
			Binder binder = context.get(Binder.class);
			if (!getDiscoveryEnabled(binder)) {
				return null;
			}

			return binder.bind(EurekaClientConfigBean.PREFIX, EurekaClientConfigBean.class)
					.orElseGet(EurekaClientConfigBean::new);
		});

		registry.registerIfAbsent(ConfigServerInstanceProvider.Function.class, context -> {
			Binder binder = context.get(Binder.class);
			if (!getDiscoveryEnabled(binder)) {
				return null;
			}
			EurekaClientConfigBean config = context.get(EurekaClientConfigBean.class);
			EurekaHttpClient httpClient = new RestTemplateTransportClientFactory(
					context.getOrElse(TlsProperties.class, null),
					context.getOrElse(EurekaClientHttpRequestFactorySupplier.class,
							new DefaultEurekaClientHttpRequestFactorySupplier()))
									.newClient(HostnameBasedUrlRandomizer.randomEndpoint(config, binder));
			return new EurekaConfigServerInstanceProvider(httpClient, config)::getInstances;
		});
	}

	private Boolean getDiscoveryEnabled(Binder binder) {
		return binder.bind(ConfigClientProperties.CONFIG_DISCOVERY_ENABLED, Boolean.class).orElse(false);
	}

}
