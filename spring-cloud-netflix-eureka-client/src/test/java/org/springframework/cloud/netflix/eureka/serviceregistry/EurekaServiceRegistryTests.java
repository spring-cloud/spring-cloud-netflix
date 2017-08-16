/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.springframework.cloud.netflix.eureka.serviceregistry;

import org.junit.Test;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.context.ApplicationEventPublisher;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Spencer Gibb
 */
public class EurekaServiceRegistryTests {

	@Test
	public void eurekaClientNotShutdownInDeregister() {
		EurekaServiceRegistry registry = new EurekaServiceRegistry();

		CloudEurekaClient eurekaClient = mock(CloudEurekaClient.class);
		ApplicationInfoManager applicationInfoManager = mock(ApplicationInfoManager.class);

		when(applicationInfoManager.getInfo()).thenReturn(mock(InstanceInfo.class));

		EurekaRegistration registration = EurekaRegistration.builder(new EurekaInstanceConfigBean(new InetUtils(new InetUtilsProperties())))
				.with(eurekaClient)
				.with(applicationInfoManager)
				.with(new EurekaClientConfigBean(), mock(ApplicationEventPublisher.class))
				.build();

		registry.deregister(registration);

		verifyZeroInteractions(eurekaClient);
	}

}
