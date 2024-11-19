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
import org.springframework.cloud.configuration.TlsProperties;
import org.springframework.cloud.netflix.eureka.http.EurekaClientHttpRequestFactorySupplier;
import org.springframework.cloud.netflix.eureka.http.RestClientDiscoveryClientOptionalArgs;
import org.springframework.cloud.netflix.eureka.http.RestClientTransportClientFactories;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.netflix.eureka.config.DiscoveryClientOptionalArgsConfiguration.setupTLS;

/**
 * Tests for verifying TLS setup with {@link RestClientTransportClientFactories}.
 *
 * @author Olga Maciaszek-Sharma
 */
public class RestClientEurekaClientTests extends BaseCertTests {

	private static final Log LOG = LogFactory.getLog(RestClientEurekaClientTests.class);

	private static EurekaServerRunner server;

	private static EurekaClientRunner service;

	@BeforeAll
	public static void setupAll() {
		server = startEurekaServer(TestEurekaServer.class);
		service = startService(server, TestApp.class);
		// Will use RestClient
		assertThat(service.discoveryClientOptionalArgs()).isInstanceOf(RestClientDiscoveryClientOptionalArgs.class);
		LOG.info("Successfully asserted that RestClient will be used");
		waitForRegistration(() -> new RestClientEurekaClientTests().createEurekaClient());
	}

	@AfterAll
	public static void tearDownAll() {
		stopService(service);
		stopEurekaServer(server);
	}

	@Override
	EurekaClientRunner createEurekaClient() {
		return new EurekaClientRunner(TestApp.class, server);
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	public static class TestApp {

		// Want to force reusing exactly the same bean as on production without excluding
		// jersey from the classpath
		@Bean
		public RestClientDiscoveryClientOptionalArgs forceRestClientDiscoveryClientOptionalArgs(
				TlsProperties tlsProperties,
				EurekaClientHttpRequestFactorySupplier eurekaClientHttpRequestFactorySupplier)
				throws GeneralSecurityException, IOException {
			RestClientDiscoveryClientOptionalArgs result = new RestClientDiscoveryClientOptionalArgs(
					eurekaClientHttpRequestFactorySupplier, RestClient::builder);
			setupTLS(result, tlsProperties);
			return result;
		}

		@Bean
		public RestClientTransportClientFactories forceRestClientTransportClientFactories(
				RestClientDiscoveryClientOptionalArgs discoveryClientOptionalArgs) {
			return new RestClientTransportClientFactories(discoveryClientOptionalArgs);
		}

	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EnableEurekaServer
	public static class TestEurekaServer {

	}

}
