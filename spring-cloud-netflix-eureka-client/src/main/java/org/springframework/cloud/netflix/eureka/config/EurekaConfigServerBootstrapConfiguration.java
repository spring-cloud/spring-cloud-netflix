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

import java.util.ArrayList;
import java.util.List;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.endpoint.EndpointUtils;
import com.netflix.discovery.shared.Applications;
import com.netflix.discovery.shared.resolver.DefaultEndpoint;
import com.netflix.discovery.shared.transport.EurekaHttpClient;
import com.netflix.discovery.shared.transport.EurekaHttpResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.http.codec.CodecsAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.config.client.ConfigServerInstanceProvider;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;
import org.springframework.cloud.netflix.eureka.http.RestTemplateEurekaHttpClient;
import org.springframework.cloud.netflix.eureka.http.RestTemplateTransportClientFactory;
import org.springframework.cloud.netflix.eureka.http.WebClientEurekaHttpClient;
import org.springframework.cloud.netflix.eureka.http.WebClientTransportClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Bootstrap configuration for config client that wants to lookup the config server via
 * discovery.
 *
 * @author Dave Syer
 */
@ConditionalOnClass(ConfigServicePropertySourceLocator.class)
@ConditionalOnProperty(value = "spring.cloud.config.discovery.enabled",
		matchIfMissing = false)
@Configuration(proxyBeanMethods = false)
public class EurekaConfigServerBootstrapConfiguration {

	private static final Log log = LogFactory
			.getLog(EurekaConfigServerBootstrapConfiguration.class);

	@Bean
	@ConditionalOnMissingBean(value = EurekaClientConfig.class,
			search = SearchStrategy.CURRENT)
	public EurekaClientConfigBean eurekaClientConfigBean() {
		return new EurekaClientConfigBean();
	}

	@Bean
	@ConditionalOnMissingBean(EurekaHttpClient.class)
	@ConditionalOnProperty(prefix = "eureka.client", name = "webclient.enabled",
			matchIfMissing = true, havingValue = "false")
	public RestTemplateEurekaHttpClient configDiscoveryRestTemplateEurekaHttpClient(
			EurekaClientConfigBean config) {
		return (RestTemplateEurekaHttpClient) new RestTemplateTransportClientFactory()
				.newClient(new DefaultEndpoint(getEurekaUrl(config)));
	}

	private static String getEurekaUrl(EurekaClientConfigBean config) {
		List<String> urls = EndpointUtils.getServiceUrlsFromConfig(config,
				EurekaClientConfigBean.DEFAULT_ZONE, true);
		return urls.get(0);
	}

	private boolean isSuccessful(EurekaHttpResponse<Applications> response) {
		HttpStatus httpStatus = HttpStatus.resolve(response.getStatusCode());
		return httpStatus != null && httpStatus.is2xxSuccessful();
	}

	@Bean
	public ConfigServerInstanceProvider.Function eurekaConfigServerInstanceProvider(
			EurekaHttpClient client, EurekaClientConfig config) {

		return serviceId -> {
			if (log.isDebugEnabled()) {
				log.debug("eurekaConfigServerInstanceProvider finding instances for "
						+ serviceId);
			}
			EurekaHttpResponse<Applications> response = client
					.getApplications(config.getRegion());
			List<ServiceInstance> instances = new ArrayList<>();
			if (!isSuccessful(response) || response.getEntity() == null) {
				return instances;
			}

			Applications applications = response.getEntity();
			applications.shuffleInstances(config.shouldFilterOnlyUpInstances());
			List<InstanceInfo> infos = applications
					.getInstancesByVirtualHostName(serviceId);
			for (InstanceInfo info : infos) {
				instances.add(new EurekaServiceInstance(info));
			}
			if (log.isDebugEnabled()) {
				log.debug("eurekaConfigServerInstanceProvider found " + infos.size()
						+ " instance(s) for " + serviceId + ", " + instances);
			}
			return instances;
		};
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(
			name = "org.springframework.web.reactive.function.client.WebClient")
	@ConditionalOnProperty(prefix = "eureka.client", name = "webclient.enabled",
			havingValue = "true")
	@ImportAutoConfiguration({ CodecsAutoConfiguration.class,
			WebClientAutoConfiguration.class })
	protected static class WebClientConfiguration {

		@Bean
		@ConditionalOnMissingBean(EurekaHttpClient.class)
		public WebClientEurekaHttpClient configDiscoveryWebClientEurekaHttpClient(
				EurekaClientConfigBean config,
				ObjectProvider<WebClient.Builder> builder) {
			return (WebClientEurekaHttpClient) new WebClientTransportClientFactory(
					builder::getIfAvailable)
							.newClient(new DefaultEndpoint(getEurekaUrl(config)));
		}

	}

}
