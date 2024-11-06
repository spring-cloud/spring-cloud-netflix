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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.BeforeAll;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.netflix.eureka.http.DefaultEurekaClientHttpRequestFactorySupplier;
import org.springframework.cloud.netflix.eureka.http.RestTemplateDiscoveryClientOptionalArgs;
import org.springframework.cloud.netflix.eureka.http.RestTemplateTransportClientFactories;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

public class EurekaClientTests extends BaseCertTests {

	private static final Log LOG = LogFactory.getLog(EurekaClientTests.class);

	static EurekaServerRunner server;

	static EurekaClientRunner service;

	@BeforeAll
	public static void setupAll() {
		server = startEurekaServer(EurekaClientTests.TestEurekaServer.class);
		service = startService(server, EurekaClientTests.TestApp.class);
		assertThat(service.discoveryClientOptionalArgs()).isInstanceOf(RestTemplateDiscoveryClientOptionalArgs.class);
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
		public RestTemplateTransportClientFactories forceRestTemplateTransportClientFactories(
				RestTemplateDiscoveryClientOptionalArgs discoveryClientOptionalArgs) {
			return new RestTemplateTransportClientFactories(discoveryClientOptionalArgs);
		}

		@Bean
		public RestTemplateDiscoveryClientOptionalArgs discoveryClientOptionalArgs() {
			return new RestTemplateDiscoveryClientOptionalArgs(
					new DefaultEurekaClientHttpRequestFactorySupplier(new RestTemplateTimeoutProperties()), null);
		}

	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EnableEurekaServer
	public static class TestEurekaServer {

	}

}
