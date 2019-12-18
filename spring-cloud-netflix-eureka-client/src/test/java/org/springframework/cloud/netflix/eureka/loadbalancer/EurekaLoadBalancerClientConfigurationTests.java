/*
 * Copyright 2015-2019 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.loadbalancer;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.netflix.eureka.loadbalancer.EurekaLoadBalancerClientConfiguration.APPROXIMATE_ZONE_FROM_HOSTNAME;
import static org.springframework.cloud.netflix.eureka.loadbalancer.EurekaLoadBalancerClientConfiguration.LOADBALANCER_ZONE;

/**
 * Tests for {@link EurekaLoadBalancerClientConfiguration}.
 *
 * @author Olga Maciaszek-Sharma
 */
class EurekaLoadBalancerClientConfigurationTests {

	private EurekaClientConfigBean eurekaClientConfig = new EurekaClientConfigBean();

	private EurekaInstanceConfigBean eurekaInstanceConfig = new EurekaInstanceConfigBean(
			new InetUtils(new InetUtilsProperties()));

	private MockEnvironment environment = new MockEnvironment();

	private EurekaLoadBalancerClientConfiguration preprocessor = new EurekaLoadBalancerClientConfiguration(
			eurekaClientConfig, eurekaInstanceConfig, environment);

	@Test
	void shouldSetZoneFromInstanceMetadata() {
		eurekaInstanceConfig.getMetadataMap().put("zone", "myZone");

		preprocessor.preprocess();

		assertThat(environment.getProperty(LOADBALANCER_ZONE)).isEqualTo("myZone");
	}

	@Test
	public void shouldSetZoneToDefaultWhenNotSetInMetadata() {
		preprocessor.preprocess();

		assertThat(environment.getProperty(LOADBALANCER_ZONE)).isEqualTo("defaultZone");
	}

	@Test
	public void shouldResolveApproximateZoneFromHost() {
		eurekaInstanceConfig.setHostname("this.is.a.test.com");
		environment.setProperty(APPROXIMATE_ZONE_FROM_HOSTNAME, "true");

		preprocessor.preprocess();

		assertThat(environment.getProperty(LOADBALANCER_ZONE)).isEqualTo("is.a.test.com");
	}

}
