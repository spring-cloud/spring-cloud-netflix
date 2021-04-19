/*
 * Copyright 2013-2020 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class EurekaServerInitializerConfigurationTest {

	@Mock
	private EurekaServerBootstrap eurekaServerBootstrapMock;

	@InjectMocks
	private EurekaServerInitializerConfiguration eurekaServerInitializerConfiguration;

	private boolean callbackCalled;

	@Before
	public void setUp() {
		callbackCalled = false;
	}

	@Test
	public void testStopWithCallbackCallsStop() {
		eurekaServerInitializerConfiguration.stop(this::setCallbackCalledTrue);

		assertThat(callbackCalled).isTrue();
		verify(eurekaServerBootstrapMock).contextDestroyed(any());
	}

	private void setCallbackCalledTrue() {
		callbackCalled = true;
	}

}
