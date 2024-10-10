/*
 * Copyright 2018-2024 the original author or authors.
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

package org.springframework.cloud.netflix.eureka;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.configuration.TlsProperties;
import org.springframework.cloud.netflix.eureka.config.DiscoveryClientOptionalArgsConfiguration;
import org.springframework.cloud.netflix.eureka.http.EurekaClientHttpRequestFactorySupplier;
import org.springframework.cloud.netflix.eureka.http.RestTemplateDiscoveryClientOptionalArgs;
import org.springframework.cloud.netflix.eureka.http.RestTemplateTransportClientFactories;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

public class RestTemplateEurekaClientTests extends BaseCertTests {

	private static final Log log = LogFactory.getLog(RestTemplateEurekaClientTests.class);

	private static EurekaServerRunner server;

	private static EurekaClientRunner service;

	@BeforeAll
	public static void setupAll() {
		server = startEurekaServer(RestTemplateEurekaClientTests.RestTemplateTestEurekaServer.class);
		service = startService(server, RestTemplateEurekaClientTests.RestTemplateTestApp.class);
		// Will use RestTemplate
		assertThat(service.discoveryClientOptionalArgs()).isInstanceOf(RestTemplateDiscoveryClientOptionalArgs.class);
		log.info("Successfully asserted that RestTemplate will be used");
		waitForRegistration(() -> new RestTemplateEurekaClientTests().createEurekaClient());
	}

	@AfterAll
	public static void tearDownAll() {
		stopService(service);
		stopEurekaServer(server);
	}

	@Override
	EurekaClientRunner createEurekaClient() {
		return new EurekaClientRunner(RestTemplateTestApp.class, server);
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	public static class RestTemplateTestApp {

		// Want to force reusing exactly the same bean as on production without excluding
		// jersey from the classpath
		@Bean
		public RestTemplateDiscoveryClientOptionalArgs forceRestTemplateDiscoveryClientOptionalArgs(
				TlsProperties tlsProperties, DiscoveryClientOptionalArgsConfiguration configuration,
				EurekaClientHttpRequestFactorySupplier eurekaClientHttpRequestFactorySupplier)
				throws GeneralSecurityException, IOException {
			return configuration.restTemplateDiscoveryClientOptionalArgs(tlsProperties,
					eurekaClientHttpRequestFactorySupplier, new RestTemplateBuilderObjectProvider());
		}

		// Want to force reusing exactly the same bean as on production without excluding
		// jersey from the classpath
		@Bean
		public RestTemplateTransportClientFactories forceRestTemplateTransportClientFactories(
				DiscoveryClientOptionalArgsConfiguration configuration,
				RestTemplateDiscoveryClientOptionalArgs discoveryClientOptionalArgs) {
			return configuration.restTemplateTransportClientFactories(discoveryClientOptionalArgs);
		}

	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EnableEurekaServer
	public static class RestTemplateTestEurekaServer {

	}

	private static class RestTemplateBuilderObjectProvider implements ObjectProvider<RestTemplateBuilder> {

		private final RestTemplateBuilder builder = new RestTemplateBuilder();

		@Override
		public RestTemplateBuilder getObject(Object... args) throws BeansException {
			return builder;
		}

		@Override
		public RestTemplateBuilder getIfAvailable() throws BeansException {
			return builder;
		}

		@Override
		public RestTemplateBuilder getIfUnique() throws BeansException {
			return builder;
		}

		@Override
		public RestTemplateBuilder getObject() throws BeansException {
			return builder;
		}

	}

}
