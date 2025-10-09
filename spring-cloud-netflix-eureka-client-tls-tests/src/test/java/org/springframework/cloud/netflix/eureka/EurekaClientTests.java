/*
 * Copyright 2018-present the original author or authors.
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

import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.BeforeAll;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.netflix.eureka.http.DefaultEurekaClientHttpRequestFactorySupplier;
import org.springframework.cloud.netflix.eureka.http.RestClientDiscoveryClientOptionalArgs;
import org.springframework.cloud.netflix.eureka.http.RestClientTransportClientFactories;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

public class EurekaClientTests extends BaseCertTests {

	private static final Log LOG = LogFactory.getLog(EurekaClientTests.class);

	static EurekaServerRunner server;

	static EurekaClientRunner service;

	@BeforeAll
	public static void setupAll() {
		server = startEurekaServer(EurekaClientTests.TestEurekaServer.class);
		service = startService(server, EurekaClientTests.TestApp.class);
		assertThat(service.discoveryClientOptionalArgs()).isInstanceOf(RestClientDiscoveryClientOptionalArgs.class);
		LOG.info("Successfully asserted that Jersey will be used");
		waitForRegistration(() -> new EurekaClientTests().createEurekaClient());
	}

	@Override
	EurekaClientRunner createEurekaClient() {
		return new EurekaClientRunner(TestApp.class, server);
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	public static class TestApp {

		@Bean
		public RestClientTransportClientFactories forceRestClientTransportClientFactories(
				RestClientDiscoveryClientOptionalArgs discoveryClientOptionalArgs) {
			return new RestClientTransportClientFactories(discoveryClientOptionalArgs);
		}

		@Bean
		public RestClientDiscoveryClientOptionalArgs discoveryClientOptionalArgs() {
			return new RestClientDiscoveryClientOptionalArgs(
					new DefaultEurekaClientHttpRequestFactorySupplier(new TimeoutProperties(), Collections.emptySet()),
					RestClient::builder);
		}

	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EnableEurekaServer
	public static class TestEurekaServer {

	}

}
