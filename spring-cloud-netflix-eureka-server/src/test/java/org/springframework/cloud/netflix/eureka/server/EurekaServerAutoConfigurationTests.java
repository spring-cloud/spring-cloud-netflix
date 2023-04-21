/*
 * Copyright 2013-2022 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.server;

import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.resources.ServerCodecs;
import com.netflix.eureka.transport.EurekaServerHttpClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EurekaServerAutoConfigurationTests {

	@Mock
	private EurekaServerConfig eurekaServerConfig;

	@Mock
	private EurekaClientConfig eurekaClientConfig;

	@Mock
	private EurekaClient eurekaClient;

	@Mock
	private InstanceRegistryProperties instanceRegistryProperties;

	@Mock
	private ServerCodecs serverCodecs;

	@Mock
	private EurekaServerHttpClientFactory eurekaServerHttpClientFactory;

	@Mock
	private EurekaInstanceConfigBean eurekaInstanceConfigBean;

	@InjectMocks
	private EurekaServerAutoConfiguration eurekaServerAutoConfiguration;

	@BeforeEach
	void setup() {
		when(eurekaServerConfig.getDeltaRetentionTimerIntervalInMs()).thenReturn(1L);
	}

	@Test
	void shouldForceEurekaClientInit() {
		eurekaServerAutoConfiguration.peerAwareInstanceRegistry(serverCodecs, eurekaServerHttpClientFactory,
				eurekaInstanceConfigBean);

		verify(eurekaClient).getApplications();
	}

	@Test
	void shouldNotForceEurekaClientInit() {
		when(eurekaInstanceConfigBean.isSkipForcedClientInitialization()).thenReturn(true);

		eurekaServerAutoConfiguration.peerAwareInstanceRegistry(serverCodecs, eurekaServerHttpClientFactory,
				eurekaInstanceConfigBean);

		verify(eurekaClient, never()).getApplications();
	}

}
