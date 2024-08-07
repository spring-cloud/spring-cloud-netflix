/*
 * Copyright 2017-2022 the original author or authors.
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
import org.junit.jupiter.api.BeforeEach;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.reactive.function.client.WebClient;

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

}
