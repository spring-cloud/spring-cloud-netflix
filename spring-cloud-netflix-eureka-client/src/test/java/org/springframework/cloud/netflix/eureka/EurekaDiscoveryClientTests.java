/*
 * Copyright 2025-present the original author or authors.
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

import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Applications;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;


/**
 * @author Mohamed Macow
 */
@ExtendWith(MockitoExtension.class)
class EurekaDiscoveryClientTests {

	@Mock
	private EurekaClient eurekaClient;

	@InjectMocks
	private EurekaDiscoveryClient client;

	@Test
	void shouldCompleteProbeWhenClientHealthy() {
		when(eurekaClient.getApplications()).thenReturn(new Applications());

		assertThatCode(() -> client.probe())
				.doesNotThrowAnyException();
	}

	@Test
	void shouldThrowProbeWhenClientThrows() {
		RuntimeException eurekaException = new RuntimeException("exception");
		when(eurekaClient.getApplications()).thenThrow(eurekaException);

		assertThatExceptionOfType(eurekaException.getClass())
				.isThrownBy(() -> client.probe())
				.withMessage(eurekaException.getMessage());
	}
}
