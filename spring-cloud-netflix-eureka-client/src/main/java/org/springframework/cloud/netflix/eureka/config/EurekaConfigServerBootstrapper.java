/*
 * Copyright 2013-2022 the original author or authors.
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
import java.util.List;

import com.netflix.discovery.shared.transport.EurekaHttpClient;
import org.apache.commons.logging.Log;

import org.springframework.boot.BootstrapContext;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.BootstrapRegistryInitializer;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServerInstanceProvider;
import org.springframework.cloud.configuration.TlsProperties;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.http.DefaultEurekaClientHttpRequestFactorySupplier;
import org.springframework.cloud.netflix.eureka.http.EurekaClientHttpRequestFactorySupplier;
import org.springframework.cloud.netflix.eureka.http.RestTemplateTransportClientFactory;
import org.springframework.util.ClassUtils;

public class EurekaConfigServerBootstrapper implements BootstrapRegistryInitializer {

	@Override
	public void initialize(BootstrapRegistry registry) {
		if (!ClassUtils.isPresent("org.springframework.cloud.config.client.ConfigServerInstanceProvider", null)) {
			return;
		}

		// It is important that we pass a lambda for the Function or else we will get a
		// ClassNotFoundException when config is not on the classpath
		registry.registerIfAbsent(ConfigServerInstanceProvider.Function.class, EurekaFunction::create);
	}

	private static Boolean getDiscoveryEnabled(Binder binder) {
		return binder.bind(ConfigClientProperties.CONFIG_DISCOVERY_ENABLED, Boolean.class).orElse(false)
				&& binder.bind("eureka.client.enabled", Boolean.class).orElse(true)
				&& binder.bind("spring.cloud.discovery.enabled", Boolean.class).orElse(true);
	}

	final static class EurekaFunction implements ConfigServerInstanceProvider.Function {

		private final BootstrapContext context;

		static EurekaFunction create(BootstrapContext context) {
			return new EurekaFunction(context);
		}

		private EurekaFunction(BootstrapContext context) {
			this.context = context;
		}

		@Override
		public List<ServiceInstance> apply(String serviceId, Binder binder, BindHandler bindHandler, Log log) {
			if (binder == null || !getDiscoveryEnabled(binder)) {
				return Collections.emptyList();
			}

			EurekaClientConfigBean config = binder.bind(EurekaClientConfigBean.PREFIX, EurekaClientConfigBean.class)
					.orElseGet(EurekaClientConfigBean::new);
			EurekaHttpClient httpClient = new RestTemplateTransportClientFactory(
					context.getOrElse(TlsProperties.class, null),
					context.getOrElse(EurekaClientHttpRequestFactorySupplier.class,
							new DefaultEurekaClientHttpRequestFactorySupplier()))
									.newClient(HostnameBasedUrlRandomizer.randomEndpoint(config, binder));
			return new EurekaConfigServerInstanceProvider(httpClient, config).getInstances(serviceId);
		}

		@Override
		public List<ServiceInstance> apply(String serviceId) {
			// This should never be called now but is here for backward
			// compatibility
			return apply(serviceId, null, null, null);
		}

	}

}
