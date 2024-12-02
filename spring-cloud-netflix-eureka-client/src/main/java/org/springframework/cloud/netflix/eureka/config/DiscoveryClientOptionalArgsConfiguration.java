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

package org.springframework.cloud.netflix.eureka.config;

import java.io.IOException;
import java.security.GeneralSecurityException;

import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import com.netflix.discovery.shared.transport.jersey.TransportClientFactories;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.configuration.SSLContextFactory;
import org.springframework.cloud.configuration.TlsProperties;
import org.springframework.cloud.netflix.eureka.RestClientTimeoutProperties;
import org.springframework.cloud.netflix.eureka.RestTemplateTimeoutProperties;
import org.springframework.cloud.netflix.eureka.http.DefaultEurekaClientHttpRequestFactorySupplier;
import org.springframework.cloud.netflix.eureka.http.EurekaClientHttpRequestFactorySupplier;
import org.springframework.cloud.netflix.eureka.http.RestClientDiscoveryClientOptionalArgs;
import org.springframework.cloud.netflix.eureka.http.RestClientTransportClientFactories;
import org.springframework.cloud.netflix.eureka.http.RestTemplateDiscoveryClientOptionalArgs;
import org.springframework.cloud.netflix.eureka.http.RestTemplateTransportClientFactories;
import org.springframework.cloud.netflix.eureka.http.WebClientDiscoveryClientOptionalArgs;
import org.springframework.cloud.netflix.eureka.http.WebClientTransportClientFactories;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Daniel Lavoie
 * @author Armin Krezovic
 * @author Olga Maciaszek-Sharma
 * @author Wonchul Heo
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ RestTemplateTimeoutProperties.class, RestClientTimeoutProperties.class })
public class DiscoveryClientOptionalArgsConfiguration {

	protected static final Log logger = LogFactory.getLog(DiscoveryClientOptionalArgsConfiguration.class);

	@Bean
	@ConfigurationProperties("eureka.client.tls")
	public TlsProperties tlsProperties() {
		return new TlsProperties();
	}

	// Visible for tests
	public static void setupTLS(AbstractDiscoveryClientOptionalArgs<?> args, TlsProperties properties)
			throws GeneralSecurityException, IOException {
		if (properties.isEnabled()) {
			SSLContextFactory factory = new SSLContextFactory(properties);
			args.setSSLContext(factory.createSSLContext());
		}
	}

	/**
	 * @deprecated {@link RestTemplate}-based implementation to be removed in favour of
	 * {@link RestClient}-based implementation.
	 */
	@Configuration(proxyBeanMethods = false)
	@Conditional(OnRestTemplatePresentAndEnabledCondition.class)
	@Deprecated
	static class RestTemplateConfiguration {

		@Bean
		@ConditionalOnMissingBean
		EurekaClientHttpRequestFactorySupplier defaultEurekaClientHttpRequestFactorySupplier(
				RestTemplateTimeoutProperties restTemplateTimeoutProperties) {
			return new DefaultEurekaClientHttpRequestFactorySupplier(restTemplateTimeoutProperties);
		}

		@Bean
		@ConditionalOnMissingBean(value = { AbstractDiscoveryClientOptionalArgs.class },
				search = SearchStrategy.CURRENT)
		public RestTemplateDiscoveryClientOptionalArgs restTemplateDiscoveryClientOptionalArgs(
				TlsProperties tlsProperties,
				EurekaClientHttpRequestFactorySupplier eurekaClientHttpRequestFactorySupplier,
				ObjectProvider<RestTemplateBuilder> restTemplateBuilders) throws GeneralSecurityException, IOException {
			if (logger.isInfoEnabled()) {
				logger.info("Eureka HTTP Client uses RestTemplate.");
			}
			RestTemplateDiscoveryClientOptionalArgs result = new RestTemplateDiscoveryClientOptionalArgs(
					eurekaClientHttpRequestFactorySupplier, restTemplateBuilders::getIfAvailable);
			setupTLS(result, tlsProperties);
			return result;
		}

		@Bean
		@ConditionalOnMissingBean(value = { TransportClientFactories.class }, search = SearchStrategy.CURRENT)
		public RestTemplateTransportClientFactories restTemplateTransportClientFactories(
				RestTemplateDiscoveryClientOptionalArgs optionalArgs) {
			return new RestTemplateTransportClientFactories(optionalArgs);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Conditional(OnJerseyClientPresentAndEnabledCondition.class)
	@ConditionalOnBean(value = AbstractDiscoveryClientOptionalArgs.class, search = SearchStrategy.CURRENT)
	static class DiscoveryClientOptionalArgsTlsConfiguration {

		DiscoveryClientOptionalArgsTlsConfiguration(TlsProperties tlsProperties,
				AbstractDiscoveryClientOptionalArgs optionalArgs) throws GeneralSecurityException, IOException {
			if (logger.isInfoEnabled()) {
				logger.info("Eureka HTTP Client uses Jersey");
			}
			setupTLS(optionalArgs, tlsProperties);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Conditional(OnJerseyClientNotPresentOrNotEnabledCondition.class)
	@ConditionalOnClass(name = "org.springframework.web.reactive.function.client.WebClient")
	@ConditionalOnProperty(prefix = "eureka.client", name = "webclient.enabled", havingValue = "true")
	protected static class WebClientConfiguration {

		@Bean
		@ConditionalOnMissingBean(
				value = { AbstractDiscoveryClientOptionalArgs.class, RestTemplateDiscoveryClientOptionalArgs.class },
				search = SearchStrategy.CURRENT)
		public WebClientDiscoveryClientOptionalArgs webClientDiscoveryClientOptionalArgs(TlsProperties tlsProperties,
				ObjectProvider<WebClient.Builder> builder) throws GeneralSecurityException, IOException {
			if (logger.isInfoEnabled()) {
				logger.info("Eureka HTTP Client uses WebClient.");
			}
			WebClientDiscoveryClientOptionalArgs result = new WebClientDiscoveryClientOptionalArgs(
					builder::getIfAvailable);
			setupTLS(result, tlsProperties);
			return result;
		}

		@Bean
		@ConditionalOnMissingBean(value = TransportClientFactories.class, search = SearchStrategy.CURRENT)
		public WebClientTransportClientFactories webClientTransportClientFactories(
				ObjectProvider<WebClient.Builder> builder) {
			return new WebClientTransportClientFactories(builder::getIfAvailable);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Conditional(OnJerseyClientNotPresentOrNotEnabledCondition.class)
	@ConditionalOnMissingClass("org.springframework.web.reactive.function.client.WebClient")
	@ConditionalOnProperty(prefix = "eureka.client", name = "webclient.enabled", havingValue = "true")
	protected static class WebClientNotFoundConfiguration {

		public WebClientNotFoundConfiguration() {
			throw new IllegalStateException(
					"eureka.client.webclient.enabled is true, " + "but WebClient is not on the classpath. Please add "
							+ "spring-boot-starter-webflux as a dependency.");
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Conditional(OnRestClientPresentAndEnabledCondition.class)
	protected static class RestClientConfiguration {

		@Bean
		@ConditionalOnMissingBean
		EurekaClientHttpRequestFactorySupplier defaultEurekaClientHttpRequestFactorySupplier(
				RestClientTimeoutProperties restClientTimeoutProperties) {
			return new DefaultEurekaClientHttpRequestFactorySupplier(restClientTimeoutProperties);
		}

		@Bean
		@ConditionalOnMissingBean(value = { AbstractDiscoveryClientOptionalArgs.class },
				search = SearchStrategy.CURRENT)
		public RestClientDiscoveryClientOptionalArgs restClientDiscoveryClientOptionalArgs(TlsProperties tlsProperties,
				EurekaClientHttpRequestFactorySupplier eurekaClientHttpRequestFactorySupplier,
				ObjectProvider<RestClient.Builder> restClientBuilderProvider)
				throws GeneralSecurityException, IOException {
			if (logger.isInfoEnabled()) {
				logger.info("Eureka HTTP Client uses RestClient.");
			}
			RestClientDiscoveryClientOptionalArgs result = new RestClientDiscoveryClientOptionalArgs(
					eurekaClientHttpRequestFactorySupplier,
					() -> restClientBuilderProvider.getIfAvailable(RestClient::builder));
			setupTLS(result, tlsProperties);
			return result;
		}

		@Bean
		@ConditionalOnMissingBean(value = TransportClientFactories.class, search = SearchStrategy.CURRENT)
		public RestClientTransportClientFactories restClientTransportClientFactories(
				RestClientDiscoveryClientOptionalArgs args) {
			return new RestClientTransportClientFactories(args);
		}

	}

	static class OnJerseyClientPresentAndEnabledCondition extends AllNestedConditions {

		OnJerseyClientPresentAndEnabledCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnClass(name = "org.glassfish.jersey.client.JerseyClient")
		static class OnJerseyClientPresent {

		}

		@ConditionalOnProperty(value = "eureka.client.jersey.enabled", matchIfMissing = true)
		static class OnJerseyClientEnabled {

		}

	}

	static class OnJerseyClientNotPresentOrNotEnabledCondition extends AnyNestedCondition {

		OnJerseyClientNotPresentOrNotEnabledCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnMissingClass("org.glassfish.jersey.client.JerseyClient")
		static class OnJerseyClientMissing {

		}

		@ConditionalOnProperty(value = "eureka.client.jersey.enabled", havingValue = "false")
		static class OnJerseyClientDisabled {

		}

	}

	/**
	 * @deprecated {@link RestTemplate}-based implementation to be removed in favour of
	 * {@link RestClient}-based implementation.
	 */
	@Deprecated(forRemoval = true)
	static class OnRestTemplatePresentAndEnabledCondition extends AllNestedConditions {

		OnRestTemplatePresentAndEnabledCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnClass(name = "org.springframework.web.client.RestTemplate")
		static class OnRestTemplatePresent {

		}

		@Conditional(OnJerseyClientNotPresentOrNotEnabledCondition.class)
		static class OnJerseyClientNotPresentOrNotEnabled {

		}

		@ConditionalOnProperty(prefix = "eureka.client", name = "webclient.enabled", matchIfMissing = true,
				havingValue = "false")
		static class OnWebClientDisabled {

		}

		@ConditionalOnProperty(prefix = "eureka.client", name = "restclient.enabled", havingValue = "false",
				matchIfMissing = true)
		static class OnRestClientDisabled {

		}

	}

	static class OnRestClientPresentAndEnabledCondition extends AllNestedConditions {

		OnRestClientPresentAndEnabledCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnClass(name = "org.springframework.web.client.RestClient")
		static class OnRestClientPresent {

		}

		@Conditional(OnJerseyClientNotPresentOrNotEnabledCondition.class)
		static class OnJerseyClientNotPresentOrNotEnabled {

		}

		@ConditionalOnProperty(prefix = "eureka.client", name = "webclient.enabled", matchIfMissing = true,
				havingValue = "false")
		static class OnWebClientDisabled {

		}

		@ConditionalOnProperty(prefix = "eureka.client", name = "restclient.enabled", havingValue = "true")
		static class OnRestClientEnabled {

		}

	}

}
