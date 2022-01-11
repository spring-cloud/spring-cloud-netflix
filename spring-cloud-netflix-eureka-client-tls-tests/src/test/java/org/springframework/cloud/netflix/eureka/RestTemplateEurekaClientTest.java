/*
 * Copyright 2018-2022 the original author or authors.
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
import org.springframework.cloud.netflix.eureka.config.DiscoveryClientOptionalArgsConfiguration;
import org.springframework.cloud.netflix.eureka.http.EurekaClientHttpRequestFactorySupplier;
import org.springframework.cloud.netflix.eureka.http.RestTemplateDiscoveryClientOptionalArgs;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

public class RestTemplateEurekaClientTest extends BaseCertTest {

	private static final Log log = LogFactory.getLog(RestTemplateEurekaClientTest.class);

	private static EurekaServerRunner server;

	private static EurekaClientRunner service;

	@BeforeAll
	public static void setupAll() {
		server = startEurekaServer(RestTemplateEurekaClientTest.RestTemplateTestEurekaServer.class);
		service = startService(server, RestTemplateEurekaClientTest.RestTemplateTestApp.class);
		// Will use RestTemplate
		assertThat(service.discoveryClientOptionalArgs()).isInstanceOf(RestTemplateDiscoveryClientOptionalArgs.class);
		log.info("Successfully asserted that RestTemplate will be used");
		waitForRegistration(() -> new RestTemplateEurekaClientTest().createEurekaClient());
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
					eurekaClientHttpRequestFactorySupplier);
		}

	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EnableEurekaServer
	public static class RestTemplateTestEurekaServer {

	}

}
