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

package org.springframework.cloud.netflix.sidecar;

import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Spencer Gibb
 */
public class LocalApplicationHealthCheckHandlerTests {

	@Mock
	private HealthIndicator healthIndicator;

	@Before
	public void setup() {
		initMocks(this);
	}

	@Test
	public void upMappingWorks() {
		assertStatus(InstanceStatus.UP, Health.up());
	}

	@Test
	public void downMappingWorks() {
		assertStatus(InstanceStatus.DOWN, Health.down());
	}

	@Test
	public void outOfServiceMappingWorks() {
		assertStatus(InstanceStatus.OUT_OF_SERVICE, Health.outOfService());
	}

	@Test
	public void unknownMappingWorks() {
		assertStatus(InstanceStatus.UNKNOWN, Health.unknown());
	}

	private void assertStatus(InstanceStatus expected, Health.Builder builder) {
		given(healthIndicator.health()).willReturn(builder.build());

		LocalApplicationHealthCheckHandler handler = new LocalApplicationHealthCheckHandler(
				healthIndicator);
		InstanceStatus status = handler.getStatus(InstanceStatus.UP);
		assertThat(status).isEqualTo(expected);
	}

}
