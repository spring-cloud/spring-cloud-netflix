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

import com.netflix.discovery.shared.resolver.DefaultEndpoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Daniel Lavoie
 */
class WebClientTransportClientFactoryTest {

	private WebClientTransportClientFactory transportClientFatory;

	@BeforeEach
	void setup() {
		transportClientFatory = new WebClientTransportClientFactory(WebClient::builder);
	}

	@Test
	void testWithoutUserInfo() {
		transportClientFatory.newClient(new DefaultEndpoint("http://localhost:8761"));
	}

	@Test
	void testInvalidUserInfo() {
		transportClientFatory.newClient(new DefaultEndpoint("http://test@localhost:8761"));
	}

	@Test
	void testUserInfo() {
		transportClientFatory.newClient(new DefaultEndpoint("http://test:test@localhost:8761"));
	}

	@AfterEach
	void shutdown() {
		transportClientFatory.shutdown();
	}

}
