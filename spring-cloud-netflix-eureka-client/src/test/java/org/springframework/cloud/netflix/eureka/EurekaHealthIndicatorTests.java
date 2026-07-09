/*
 * Copyright 2026-present the original author or authors.
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

package org.springframework.cloud.netflix.eureka;

import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.Applications;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link EurekaHealthIndicator}.
 *
 * @author Lucas Ma
 */
@ExtendWith(MockitoExtension.class)
class EurekaHealthIndicatorTests {

	@Mock
	private DiscoveryClient eurekaClient;

	@Mock
	private EurekaInstanceConfig instanceConfig;

	@Mock
	private EurekaClientConfig clientConfig;

	@Test
	void reportsDownWhenRegistryHasNeverBeenFetched() {
		when(this.eurekaClient.getInstanceRemoteStatus()).thenReturn(InstanceInfo.InstanceStatus.UP);
		when(this.eurekaClient.getLastSuccessfulRegistryFetchTimePeriod()).thenReturn(-1L);
		when(this.eurekaClient.getApplications()).thenReturn(new Applications());
		when(this.clientConfig.shouldFetchRegistry()).thenReturn(true);

		Health health = healthIndicator().health();

		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
	}

	@Test
	void reportsDownWhenRegistryFetchIsStale() {
		when(this.eurekaClient.getInstanceRemoteStatus()).thenReturn(InstanceInfo.InstanceStatus.UP);
		when(this.eurekaClient.getLastSuccessfulRegistryFetchTimePeriod()).thenReturn(61_000L);
		when(this.eurekaClient.getApplications()).thenReturn(new Applications());
		when(this.clientConfig.shouldFetchRegistry()).thenReturn(true);
		when(this.clientConfig.getRegistryFetchIntervalSeconds()).thenReturn(30);
		when(this.instanceConfig.getLeaseRenewalIntervalInSeconds()).thenReturn(30);

		Health health = healthIndicator().health();

		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).containsEntry("renewalPeriod", 30);
		assertThat(health.getDetails()).containsEntry("failCount", 2033L);
	}

	private EurekaHealthIndicator healthIndicator() {
		return new EurekaHealthIndicator(this.eurekaClient, this.instanceConfig, this.clientConfig);
	}

}
