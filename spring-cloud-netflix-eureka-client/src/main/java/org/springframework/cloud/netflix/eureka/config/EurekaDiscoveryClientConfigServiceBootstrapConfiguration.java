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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.AzToRegionMapper;
import com.netflix.discovery.DNSBasedAzToRegionMapper;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.InstanceRegionChecker;
import com.netflix.discovery.PropertyBasedAzToRegionMapper;
import com.netflix.discovery.endpoint.EndpointUtils;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import com.netflix.discovery.shared.resolver.DefaultEndpoint;
import com.netflix.discovery.shared.transport.EurekaHttpResponse;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;
import org.springframework.cloud.netflix.eureka.http.RestTemplateEurekaHttpClient;
import org.springframework.cloud.netflix.eureka.http.RestTemplateTransportClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.util.ReflectionUtils;

/**
 * Eureka-specific helper for config client that wants to lookup the config server via
 * discovery.
 *
 * @author Dave Syer
 */
@ConditionalOnClass(ConfigServicePropertySourceLocator.class)
@ConditionalOnProperty(value = "spring.cloud.config.discovery.enabled",
		matchIfMissing = false)
@Configuration(proxyBeanMethods = false)
public class EurekaDiscoveryClientConfigServiceBootstrapConfiguration {

	@Bean
	@ConditionalOnMissingBean(value = EurekaClientConfig.class,
			search = SearchStrategy.CURRENT)
	public EurekaClientConfigBean eurekaClientConfigBean() {
		return new EurekaClientConfigBean();
	}

	@Bean
	public RestTemplateEurekaHttpClient configDiscoveryWebClientEurekaHttpClient(
			EurekaClientConfigBean config) {
		List<String> urls = EndpointUtils.getServiceUrlsFromConfig(config, "unknown",
				true);
		String url = urls.get(0);
		return (RestTemplateEurekaHttpClient) new RestTemplateTransportClientFactory()
				.newClient(new DefaultEndpoint(url));
	}

	private boolean isSuccessful(EurekaHttpResponse<Applications> response) {
		return HttpStatus.resolve(response.getStatusCode()).is2xxSuccessful();
	}

	@Bean
	// TODO: only need blocking since ConfigServerInstanceProvider expects only it.
	public DiscoveryClient configDiscoveryDiscoveryClient(
			RestTemplateEurekaHttpClient client, EurekaClientConfig clientConfig) {
		return new DiscoveryClient() {

			@Override
			public String description() {
				return "configDiscoveryDiscoveryClient";
			}

			@Override
			public List<ServiceInstance> getInstances(String serviceId) {
				AzToRegionMapper azToRegionMapper;
				if (clientConfig.shouldUseDnsForFetchingServiceUrls()) {
					azToRegionMapper = new DNSBasedAzToRegionMapper(clientConfig);
				}
				else {
					azToRegionMapper = new PropertyBasedAzToRegionMapper(clientConfig);
				}
				String localRegion = null;
				try {
					Constructor<InstanceRegionChecker> constructor = ReflectionUtils
							.accessibleConstructor(InstanceRegionChecker.class,
									AzToRegionMapper.class, String.class);

					InstanceRegionChecker instanceRegionChecker = constructor
							.newInstance(azToRegionMapper, clientConfig.getRegion());
					localRegion = instanceRegionChecker.getLocalRegion();
				}
				catch (Exception e) {
					ReflectionUtils.rethrowRuntimeException(e);
				}

				EurekaHttpResponse<Applications> response = client
						.getApplications(localRegion);
				List<ServiceInstance> instances = new ArrayList<>();
				if (!isSuccessful(response) || response.getEntity() == null) {
					return instances;
				}

				Applications applications = response.getEntity();
				applications.shuffleInstances(clientConfig.shouldFilterOnlyUpInstances());
				List<InstanceInfo> infos = applications
						.getInstancesByVirtualHostName(serviceId);
				for (InstanceInfo info : infos) {
					instances.add(new EurekaServiceInstance(info));
				}
				return instances;
			}

			@Override
			public List<String> getServices() {
				EurekaHttpResponse<Applications> response = client.getApplications();
				if (!isSuccessful(response) || response.getEntity() == null) {
					return Collections.emptyList();
				}
				List<Application> registered = response.getEntity()
						.getRegisteredApplications();
				List<String> names = new ArrayList<>();
				for (Application app : registered) {
					if (app.getInstances().isEmpty()) {
						continue;
					}
					names.add(app.getName().toLowerCase());

				}
				return names;
			}

		};
	}

}
