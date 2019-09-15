/*
 * Copyright 2019-2019 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.reactive;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author Tim Ysewyn
 */
@ExtendWith(MockitoExtension.class)
class EurekaReactiveDiscoveryClientTests {

	@Mock
	private EurekaClient eurekaClient;

	@Mock
	private EurekaClientConfig clientConfig;

	@InjectMocks
	private EurekaReactiveDiscoveryClient client;

	@Test
	public void verifyDefaults() {
		assertThat(client.description())
				.isEqualTo("Spring Cloud Eureka Reactive Discovery Client");
		assertThat(client.getOrder()).isEqualTo(ReactiveDiscoveryClient.DEFAULT_ORDER);
	}

	@Test
	public void verifyDefaultsWhenUsingEurekaClientConfigBean() {
		EurekaClientConfigBean configBean = new EurekaClientConfigBean();
		configBean.setOrder(1);
		EurekaReactiveDiscoveryClient client = new EurekaReactiveDiscoveryClient(
				eurekaClient, configBean);
		assertThat(client.description())
				.isEqualTo("Spring Cloud Eureka Reactive Discovery Client");
		assertThat(client.getOrder()).isEqualTo(1);
	}

	@Test
	public void shouldReturnEmptyFluxOfServices() {
		when(eurekaClient.getApplications()).thenReturn(new Applications());
		Flux<String> services = this.client.getServices();
		StepVerifier.create(services).expectNextCount(0).expectComplete().verify();
	}

	@Test
	public void shouldReturnEmptyFluxOfServicesWhenNoInstancesFound() {
		Applications applications = new Applications();
		applications.addApplication(new Application("my-service"));
		when(eurekaClient.getApplications()).thenReturn(applications);
		Flux<String> services = this.client.getServices();
		StepVerifier.create(services).expectNextCount(0).expectComplete().verify();
	}

	@Test
	public void shouldReturnFluxOfServices() {
		Applications applications = new Applications();
		Application app = new Application("my-service");
		app.addInstance(new InstanceInfo("instance", "my-service", "", "127.0.0.1", "",
				null, null, "", "", "", "", "", "", 0, null, "", null, null, null, null,
				null, null, null, null, null, null));
		applications.addApplication(app);
		when(eurekaClient.getApplications()).thenReturn(applications);
		Flux<String> services = this.client.getServices();
		StepVerifier.create(services).expectNext("my-service").expectComplete().verify();
	}

	@Test
	public void shouldReturnEmptyFluxForNonExistingService() {
		when(eurekaClient.getInstancesByVipAddress("nonexistent-service", false))
				.thenReturn(emptyList());
		Flux<ServiceInstance> instances = this.client.getInstances("nonexistent-service");
		StepVerifier.create(instances).expectNextCount(0).expectComplete().verify();
	}

	@Test
	public void shouldReturnFluxOfServiceInstances() {
		InstanceInfo instanceInfo = new InstanceInfo(new InstanceInfo("instance",
				"my-service", "", "127.0.0.1", "", null, null, "", "", "", "", "", "", 0,
				null, "", null, null, null, null, null, null, null, null, null, null));
		when(eurekaClient.getInstancesByVipAddress("my-service", false))
				.thenReturn(singletonList(instanceInfo));
		Flux<ServiceInstance> instances = this.client.getInstances("my-service");
		StepVerifier.create(instances).expectNextCount(1).expectComplete().verify();
	}

}
