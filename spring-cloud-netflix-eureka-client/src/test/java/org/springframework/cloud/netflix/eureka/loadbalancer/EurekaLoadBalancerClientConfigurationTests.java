/*
 * Copyright 2015-2020 the original author or authors.
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
import org.springframework.cloud.loadbalancer.config.LoadBalancerZoneConfig;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EurekaLoadBalancerClientConfiguration}.
 *
 * @author Olga Maciaszek-Sharma
 */
class EurekaLoadBalancerClientConfigurationTests {

	private EurekaClientConfigBean eurekaClientConfig = new EurekaClientConfigBean();

	private EurekaInstanceConfigBean eurekaInstanceConfig = new EurekaInstanceConfigBean(
			new InetUtils(new InetUtilsProperties()));

	private LoadBalancerZoneConfig zoneConfig = new LoadBalancerZoneConfig(null);

	private EurekaLoadBalancerProperties eurekaLoadBalancerProperties = new EurekaLoadBalancerProperties();

	private EurekaLoadBalancerClientConfiguration postprocessor = new EurekaLoadBalancerClientConfiguration(
			eurekaClientConfig, eurekaInstanceConfig, zoneConfig,
			eurekaLoadBalancerProperties);

	@Test
	void shouldSetZoneFromInstanceMetadata() {
		eurekaInstanceConfig.getMetadataMap().put("zone", "myZone");

		postprocessor.postprocess();

		assertThat(zoneConfig.getZone()).isEqualTo("myZone");
	}

	@Test
	public void shouldSetZoneToDefaultWhenNotSetInMetadata() {
		postprocessor.postprocess();

		assertThat(zoneConfig.getZone()).isEqualTo("defaultZone");
	}

	@Test
	public void shouldResolveApproximateZoneFromHost() {
		eurekaInstanceConfig.setHostname("this.is.a.test.com");
		eurekaLoadBalancerProperties.setApproximateZoneFromHostname(true);

		postprocessor.postprocess();

		assertThat(zoneConfig.getZone()).isEqualTo("is.a.test.com");
	}

}
