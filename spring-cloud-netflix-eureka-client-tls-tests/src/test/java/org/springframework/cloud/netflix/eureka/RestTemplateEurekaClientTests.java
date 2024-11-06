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

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.configuration.TlsProperties;
import org.springframework.cloud.netflix.eureka.http.DefaultEurekaClientHttpRequestFactorySupplier;
import org.springframework.cloud.netflix.eureka.http.EurekaClientHttpRequestFactorySupplier;
import org.springframework.cloud.netflix.eureka.http.RestTemplateDiscoveryClientOptionalArgs;
import org.springframework.cloud.netflix.eureka.http.RestTemplateTransportClientFactories;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.netflix.eureka.config.DiscoveryClientOptionalArgsConfiguration.setupTLS;

public class RestTemplateEurekaClientTests extends BaseCertTests {

	private static final Log log = LogFactory.getLog(RestTemplateEurekaClientTests.class);

	private static EurekaServerRunner server;

	private static EurekaClientRunner service;

	@BeforeAll
	public static void setupAll() {
		server = startEurekaServer(TestEurekaServer.class);
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

		@Bean
		public RestTemplateDiscoveryClientOptionalArgs forceRestTemplateDiscoveryClientOptionalArgs(
				TlsProperties tlsProperties,
				EurekaClientHttpRequestFactorySupplier eurekaClientHttpRequestFactorySupplier)
				throws GeneralSecurityException, IOException {
			RestTemplateDiscoveryClientOptionalArgs result = new RestTemplateDiscoveryClientOptionalArgs(
					eurekaClientHttpRequestFactorySupplier, RestTemplateBuilder::new);
			setupTLS(result, tlsProperties);
			return result;
		}

		@Bean
		public RestTemplateTransportClientFactories forceRestTemplateTransportClientFactories(
				RestTemplateDiscoveryClientOptionalArgs discoveryClientOptionalArgs) {
			return new RestTemplateTransportClientFactories(discoveryClientOptionalArgs);
		}

		@Bean
		EurekaClientHttpRequestFactorySupplier defaultEurekaClientHttpRequestFactorySupplier(
				RestTemplateTimeoutProperties restTemplateTimeoutProperties) {
			return new DefaultEurekaClientHttpRequestFactorySupplier(restTemplateTimeoutProperties);
		}

	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EnableEurekaServer
	public static class TestEurekaServer {

	}

}
