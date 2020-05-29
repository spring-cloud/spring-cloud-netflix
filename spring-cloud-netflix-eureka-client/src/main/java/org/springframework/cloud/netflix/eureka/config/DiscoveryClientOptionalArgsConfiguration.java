/*
 * Copyright 2017-2020 the original author or authors.
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

import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.cloud.netflix.eureka.MutableDiscoveryClientOptionalArgs;
import org.springframework.cloud.netflix.eureka.http.RestTemplateDiscoveryClientOptionalArgs;
import org.springframework.cloud.netflix.eureka.http.WebClientDiscoveryClientOptionalArgs;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Daniel Lavoie
 */
@Configuration(proxyBeanMethods = false)
public class DiscoveryClientOptionalArgsConfiguration {

	protected final Log logger = LogFactory.getLog(getClass());

	@Bean
	@ConditionalOnClass(name = "org.springframework.web.client.RestTemplate")
	@ConditionalOnMissingClass("com.sun.jersey.api.client.filter.ClientFilter")
	@ConditionalOnMissingBean(value = { AbstractDiscoveryClientOptionalArgs.class },
			search = SearchStrategy.CURRENT)
	@ConditionalOnProperty(prefix = "eureka.client", name = "webclient.enabled",
			matchIfMissing = true, havingValue = "false")
	public RestTemplateDiscoveryClientOptionalArgs restTemplateDiscoveryClientOptionalArgs() {
		logger.info("Eureka HTTP Client uses RestTemplate.");
		return new RestTemplateDiscoveryClientOptionalArgs();
	}

	@Bean
	@ConditionalOnMissingClass("com.sun.jersey.api.client.filter.ClientFilter")
	@ConditionalOnClass(
			name = "org.springframework.web.reactive.function.client.WebClient")
	@ConditionalOnMissingBean(
			value = { AbstractDiscoveryClientOptionalArgs.class,
					RestTemplateDiscoveryClientOptionalArgs.class },
			search = SearchStrategy.CURRENT)
	@ConditionalOnProperty(prefix = "eureka.client", name = "webclient.enabled",
			havingValue = "true")
	public WebClientDiscoveryClientOptionalArgs webClientDiscoveryClientOptionalArgs() {
		logger.info("Eureka HTTP Client uses WebClient.");
		return new WebClientDiscoveryClientOptionalArgs();
	}

	@Bean
	@ConditionalOnClass(name = "com.sun.jersey.api.client.filter.ClientFilter")
	@ConditionalOnMissingBean(value = AbstractDiscoveryClientOptionalArgs.class,
			search = SearchStrategy.CURRENT)
	public MutableDiscoveryClientOptionalArgs discoveryClientOptionalArgs() {
		logger.info("Eureka Client uses Jersey");
		return new MutableDiscoveryClientOptionalArgs();
	}

	@Configuration
	@ConditionalOnMissingClass({ "com.sun.jersey.api.client.filter.ClientFilter",
			"org.springframework.web.reactive.function.client.WebClient" })
	@ConditionalOnProperty(prefix = "eureka.client", name = "webclient.enabled",
			havingValue = "true")
	protected static class WebClientNotFoundConfiguration {

		public WebClientNotFoundConfiguration() {
			throw new IllegalStateException("eureka.client.webclient.enabled is true, "
					+ "but WebClient is not on the classpath. Please add "
					+ "spring-boot-starter-webflux as a dependency.");
		}

	}

}
