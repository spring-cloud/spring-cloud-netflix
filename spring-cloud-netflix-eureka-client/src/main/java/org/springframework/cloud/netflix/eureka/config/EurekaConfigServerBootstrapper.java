/*
 * Copyright 2013-2024 the original author or authors.
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

import java.util.Collections;

import com.netflix.discovery.shared.transport.EurekaHttpClient;

import org.springframework.boot.BootstrapContext;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.BootstrapRegistryInitializer;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServerConfigDataLocationResolver.PropertyResolver;
import org.springframework.cloud.config.client.ConfigServerInstanceProvider;
import org.springframework.cloud.configuration.TlsProperties;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.RestClientTimeoutProperties;
import org.springframework.cloud.netflix.eureka.http.DefaultEurekaClientHttpRequestFactorySupplier;
import org.springframework.cloud.netflix.eureka.http.EurekaClientHttpRequestFactorySupplier;
import org.springframework.cloud.netflix.eureka.http.RestClientTransportClientFactory;
import org.springframework.util.ClassUtils;

public class EurekaConfigServerBootstrapper implements BootstrapRegistryInitializer {

	@Override
	public void initialize(BootstrapRegistry registry) {
		if (!ClassUtils.isPresent("org.springframework.cloud.config.client.ConfigServerInstanceProvider", null)) {
			return;
		}

		registry.registerIfAbsent(EurekaClientConfigBean.class, context -> {
			if (!getDiscoveryEnabled(context)) {
				return null;
			}
			PropertyResolver propertyResolver = getPropertyResolver(context);
			return propertyResolver.resolveConfigurationProperties(EurekaClientConfigBean.PREFIX,
					EurekaClientConfigBean.class, EurekaClientConfigBean::new);
		});

		registry.registerIfAbsent(ConfigServerInstanceProvider.Function.class, context -> {
			if (!getDiscoveryEnabled(context)) {
				return (id) -> Collections.emptyList();
			}
			EurekaClientConfigBean config = context.get(EurekaClientConfigBean.class);
			EurekaHttpClient httpClient = new RestClientTransportClientFactory(
					context.getOrElse(TlsProperties.class, null),
					context.getOrElse(EurekaClientHttpRequestFactorySupplier.class,
							new DefaultEurekaClientHttpRequestFactorySupplier(new RestClientTimeoutProperties())))
				.newClient(HostnameBasedUrlRandomizer.randomEndpoint(config, getPropertyResolver(context)));
			return new EurekaConfigServerInstanceProvider(httpClient, config)::getInstances;
		});
	}

	private static PropertyResolver getPropertyResolver(BootstrapContext context) {
		return context.getOrElseSupply(PropertyResolver.class,
				() -> new PropertyResolver(context.get(Binder.class), context.getOrElse(BindHandler.class, null)));
	}

	public static Boolean getDiscoveryEnabled(BootstrapContext bootstrapContext) {
		PropertyResolver propertyResolver = getPropertyResolver(bootstrapContext);
		return propertyResolver.get(ConfigClientProperties.CONFIG_DISCOVERY_ENABLED, Boolean.class, false)
				&& propertyResolver.get("eureka.client.enabled", Boolean.class, true)
				&& propertyResolver.get("spring.cloud.discovery.enabled", Boolean.class, true);
	}

}
