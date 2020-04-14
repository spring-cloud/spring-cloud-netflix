/*
 * Copyright 2017-2019 the original author or authors.
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Daniel Lavoie
 * @author Haytham Mohamed
 */
public class RestTemplateTransportClientFactoryTest {

	private WebClientTransportClientFactory transportClientFatory;

	@Before
	public void setup() {
		transportClientFatory = new WebClientTransportClientFactory();
	}

	@Test
	public void testWithoutUserInfo() {
		transportClientFatory.newClient(new DefaultEndpoint("http://localhost:8761"));
	}

	@Test
	public void testInvalidUserInfo() {
		transportClientFatory
				.newClient(new DefaultEndpoint("http://test@localhost:8761"));
	}

	@Test
	public void testUserInfo() {
		transportClientFatory
				.newClient(new DefaultEndpoint("http://test:test@localhost:8761"));
	}

	@After
	public void shutdown() {
		transportClientFatory.shutdown();
	}

}
