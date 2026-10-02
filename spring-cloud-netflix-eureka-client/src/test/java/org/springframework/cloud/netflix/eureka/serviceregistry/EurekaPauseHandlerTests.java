/*
 * Copyright 2013-present the original author or authors.
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

package org.springframework.cloud.netflix.eureka.serviceregistry;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.context.ApplicationEventPublisher;

import static com.netflix.appinfo.InstanceInfo.InstanceStatus.OUT_OF_SERVICE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Akhil CH
 */
class EurekaPauseHandlerTests {

	private final CloudEurekaClient eurekaClient = mock(CloudEurekaClient.class);

	private final InstanceInfo instanceInfo = mock(InstanceInfo.class);

	private EurekaPauseHandler pauseHandler;

	@BeforeEach
	void setup() {
		ApplicationInfoManager applicationInfoManager = mock(ApplicationInfoManager.class);
		when(applicationInfoManager.getInfo()).thenReturn(this.instanceInfo);

		EurekaRegistration registration = EurekaRegistration
			.builder(new EurekaInstanceConfigBean(new InetUtils(new InetUtilsProperties())))
			.with(this.eurekaClient)
			.with(applicationInfoManager)
			.with(new EurekaClientConfigBean(), mock(ApplicationEventPublisher.class))
			.build();

		this.pauseHandler = new EurekaPauseHandler(new EurekaServiceRegistry(), registration);
	}

	@Test
	void pauseSetsInstanceOutOfService() {
		this.pauseHandler.pause();

		verify(this.eurekaClient).setStatus(OUT_OF_SERVICE, this.instanceInfo);
		verifyNoMoreInteractions(this.eurekaClient);
	}

	@Test
	void resumeCancelsStatusOverride() {
		this.pauseHandler.resume();

		verify(this.eurekaClient).cancelOverrideStatus(this.instanceInfo);
		verifyNoMoreInteractions(this.eurekaClient);
	}

}
