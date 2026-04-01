/*
 * Copyright 2017-present the original author or authors.
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

package org.springframework.cloud.netflix.eureka.http;

import com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider;
import com.netflix.discovery.shared.resolver.DefaultEndpoint;
import com.netflix.discovery.shared.transport.EurekaHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.LoopResources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Daniel Lavoie
 */
@SpringBootTest(classes = EurekaServerMockApplication.class,
		properties = { "debug=true", "security.basic.enabled=true", "eureka.client.webclient.enabled=true",
				"eureka.client.fetch-registry=false", "eureka.client.register-with-eureka=false" },
		webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
class WebClientEurekaHttpClientTests extends AbstractEurekaHttpClientTests {

	@Autowired
	private InetUtils inetUtils;

	@Value("http://${security.user.name}:${security.user.password}@localhost:${local.server.port}/eureka/")
	private String serviceUrl;

	@BeforeEach
	void setup() {
		eurekaHttpClient = new WebClientTransportClientFactory(WebClient::builder)
			.newClient(new DefaultEndpoint(serviceUrl));

		EurekaInstanceConfigBean config = new EurekaInstanceConfigBean(inetUtils);

		String appname = "customapp";
		config.setIpAddress("127.0.0.1");
		config.setHostname("localhost");
		config.setAppname(appname);
		config.setVirtualHostName(appname);
		config.setSecureVirtualHostName(appname);
		config.setNonSecurePort(4444);
		config.setSecurePort(8443);
		config.setInstanceId("127.0.0.1:customapp:4444");

		info = new EurekaConfigBasedInstanceInfoProvider(config).get();
	}

	@Test
	void cancelSucceedsAfterSharedReactorResourcesDisposed() {
		// Simulate the LoopResources shared between the reactive web server and a
		// WebClient (as Spring Boot's ReactorResourceFactory provides by default)
		LoopResources sharedResources = LoopResources.create("simulated-server");
		WebClient.Builder sharedBuilder = WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(HttpClient.create().runOn(sharedResources)));

		WebClientTransportClientFactory factory = new WebClientTransportClientFactory(() -> sharedBuilder);
		EurekaHttpClient client = factory.newClient(new DefaultEndpoint(serviceUrl));

		// Simulate the reactive web server shutting down (terminates the shared event
		// loop)
		sharedResources.dispose();

		// cancel() must succeed because the factory uses its own dedicated Reactor Netty
		// resources that are independent of the shared (now disposed) web server
		// resources.
		// Without the fix this would throw RejectedExecutionException: event executor
		// terminated
		assertThat(client.cancel("test", "test").getStatusCode()).isEqualTo(HttpStatus.OK.value());

		factory.shutdown();
	}

}
