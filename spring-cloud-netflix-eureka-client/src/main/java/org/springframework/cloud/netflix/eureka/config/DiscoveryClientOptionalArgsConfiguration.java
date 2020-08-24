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

import java.io.IOException;
import java.security.GeneralSecurityException;

import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.configuration.SSLContextFactory;
import org.springframework.cloud.configuration.TlsProperties;
import org.springframework.cloud.netflix.eureka.MutableDiscoveryClientOptionalArgs;
import org.springframework.cloud.netflix.eureka.http.RestTemplateDiscoveryClientOptionalArgs;
import org.springframework.cloud.netflix.eureka.http.WebClientDiscoveryClientOptionalArgs;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Daniel Lavoie
 */
@Configuration(proxyBeanMethods = false)
public class DiscoveryClientOptionalArgsConfiguration {

	protected static final Log logger = LogFactory
			.getLog(DiscoveryClientOptionalArgsConfiguration.class);

	@Bean
	@ConfigurationProperties("eureka.client.tls")
	public TlsProperties tlsProperties() {
		return new TlsProperties();
	}

	@Bean
	@ConditionalOnMissingClass("com.sun.jersey.api.client.filter.ClientFilter")
	@ConditionalOnMissingBean(value = { AbstractDiscoveryClientOptionalArgs.class },
			search = SearchStrategy.CURRENT)
	@ConditionalOnProperty(prefix = "eureka.client", name = "webclient.enabled",
			matchIfMissing = true, havingValue = "false")
	public RestTemplateDiscoveryClientOptionalArgs restTemplateDiscoveryClientOptionalArgs(
			TlsProperties tlsProperties) throws GeneralSecurityException, IOException {
		logger.info("Eureka HTTP Client uses RestTemplate.");
		RestTemplateDiscoveryClientOptionalArgs result = new RestTemplateDiscoveryClientOptionalArgs();
		setupTLS(result, tlsProperties);
		return result;
	}

	@Bean
	@ConditionalOnClass(name = "com.sun.jersey.api.client.filter.ClientFilter")
	@ConditionalOnMissingBean(value = AbstractDiscoveryClientOptionalArgs.class,
			search = SearchStrategy.CURRENT)
	public MutableDiscoveryClientOptionalArgs discoveryClientOptionalArgs(
			TlsProperties tlsProperties) throws GeneralSecurityException, IOException {
		logger.info("Eureka HTTP Client uses Jersey");
		MutableDiscoveryClientOptionalArgs result = new MutableDiscoveryClientOptionalArgs();
		setupTLS(result, tlsProperties);
		return result;
	}

	private static void setupTLS(AbstractDiscoveryClientOptionalArgs<?> args,
			TlsProperties properties) throws GeneralSecurityException, IOException {
		if (properties.isEnabled()) {
			SSLContextFactory factory = new SSLContextFactory(properties);
			args.setSSLContext(factory.createSSLContext());
		}
	}

	@ConditionalOnMissingClass("com.sun.jersey.api.client.filter.ClientFilter")
	@ConditionalOnClass(
			name = "org.springframework.web.reactive.function.client.WebClient")
	@ConditionalOnProperty(prefix = "eureka.client", name = "webclient.enabled",
			havingValue = "true")
	protected static class WebClientConfiguration {

		@Autowired
		private TlsProperties tlsProperties;

		@Bean
		@ConditionalOnMissingBean(
				value = { AbstractDiscoveryClientOptionalArgs.class,
						RestTemplateDiscoveryClientOptionalArgs.class },
				search = SearchStrategy.CURRENT)
		public WebClientDiscoveryClientOptionalArgs webClientDiscoveryClientOptionalArgs(
				ObjectProvider<WebClient.Builder> builder)
				throws GeneralSecurityException, IOException {
			logger.info("Eureka HTTP Client uses WebClient.");
			WebClientDiscoveryClientOptionalArgs result = new WebClientDiscoveryClientOptionalArgs(
					builder::getIfAvailable);
			setupTLS(result, tlsProperties);
			return result;
		}

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
