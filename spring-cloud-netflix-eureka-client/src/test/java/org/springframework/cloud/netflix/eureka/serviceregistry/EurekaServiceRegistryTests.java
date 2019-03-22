/*
 * Copyright 2013-2019 the original author or authors.
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

import java.util.Map;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import org.junit.Test;

import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.context.ApplicationEventPublisher;

import static com.netflix.appinfo.InstanceInfo.InstanceStatus.DOWN;
import static com.netflix.appinfo.InstanceInfo.InstanceStatus.OUT_OF_SERVICE;
import static com.netflix.appinfo.InstanceInfo.InstanceStatus.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Spencer Gibb
 * @author Tim Ysewyn
 */
public class EurekaServiceRegistryTests {

	@Test
	public void eurekaClientNotShutdownInDeregister() {
		EurekaServiceRegistry registry = new EurekaServiceRegistry();

		CloudEurekaClient eurekaClient = mock(CloudEurekaClient.class);
		ApplicationInfoManager applicationInfoManager = mock(
				ApplicationInfoManager.class);

		when(applicationInfoManager.getInfo()).thenReturn(mock(InstanceInfo.class));

		EurekaRegistration registration = EurekaRegistration
				.builder(new EurekaInstanceConfigBean(
						new InetUtils(new InetUtilsProperties())))
				.with(eurekaClient).with(applicationInfoManager)
				.with(new EurekaClientConfigBean(), mock(ApplicationEventPublisher.class))
				.build();

		registry.deregister(registration);

		verifyZeroInteractions(eurekaClient);
	}

	@Test
	public void eurekaClientGetStatus() {
		EurekaServiceRegistry registry = new EurekaServiceRegistry();

		EurekaInstanceConfigBean config = new EurekaInstanceConfigBean(
				new InetUtils(new InetUtilsProperties()));
		config.setAppname("myapp");
		config.setInstanceId("1234");

		InstanceInfo local = InstanceInfo.Builder.newBuilder().setAppName("myapp")
				.setInstanceId("1234").setStatus(DOWN).build();

		InstanceInfo remote = InstanceInfo.Builder.newBuilder().setAppName("myapp")
				.setInstanceId("1234").setStatus(DOWN).setOverriddenStatus(OUT_OF_SERVICE)
				.build();

		CloudEurekaClient eurekaClient = mock(CloudEurekaClient.class);
		when(eurekaClient.getInstanceInfo(local.getAppName(), local.getId()))
				.thenReturn(remote);

		ApplicationInfoManager applicationInfoManager = mock(
				ApplicationInfoManager.class);
		when(applicationInfoManager.getInfo()).thenReturn(local);

		EurekaRegistration registration = EurekaRegistration.builder(config)
				.with(eurekaClient).with(applicationInfoManager)
				.with(new EurekaClientConfigBean(), mock(ApplicationEventPublisher.class))
				.build();

		Object status = registry.getStatus(registration);

		assertThat(registration.getInstanceId()).isEqualTo("1234");

		assertThat(status).isInstanceOf(Map.class);

		Map<Object, Object> map = (Map<Object, Object>) status;

		assertThat(map).hasSize(2).containsEntry("status", DOWN.toString())
				.containsEntry("overriddenStatus", OUT_OF_SERVICE.toString());
	}

	@Test
	public void eurekaClientGetStatusNoInstance() {
		EurekaServiceRegistry registry = new EurekaServiceRegistry();

		EurekaInstanceConfigBean config = new EurekaInstanceConfigBean(
				new InetUtils(new InetUtilsProperties()));
		config.setAppname("myapp");
		config.setInstanceId("1234");

		CloudEurekaClient eurekaClient = mock(CloudEurekaClient.class);

		when(eurekaClient.getInstanceInfo("myapp", "1234")).thenReturn(null);

		ApplicationInfoManager applicationInfoManager = mock(
				ApplicationInfoManager.class);
		when(applicationInfoManager.getInfo()).thenReturn(mock(InstanceInfo.class));

		EurekaRegistration registration = EurekaRegistration.builder(config)
				.with(eurekaClient).with(applicationInfoManager)
				.with(new EurekaClientConfigBean(), mock(ApplicationEventPublisher.class))
				.build();

		Object status = registry.getStatus(registration);

		assertThat(registration.getInstanceId()).isEqualTo("1234");

		assertThat(status).isInstanceOf(Map.class);

		Map<Object, Object> map = (Map<Object, Object>) status;

		assertThat(map).hasSize(1).containsEntry("status", UNKNOWN.toString());
	}

}
