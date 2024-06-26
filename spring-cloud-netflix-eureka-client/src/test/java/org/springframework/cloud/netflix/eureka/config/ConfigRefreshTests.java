/*
 * Copyright 2013-2024 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.config;

import com.netflix.discovery.EurekaClient;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.cloud.netflix.eureka.sample.RefreshEurekaSampleApplication;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Ryan Baxter
 * @author Kaiyao Ke
 */
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = RefreshEurekaSampleApplication.class)
class ConfigRefreshTests {

	@Autowired
	private ApplicationEventPublisher publisher;

	@Autowired
	// Mocked in RefreshEurekaSampleApplication
	private EurekaClient client;

	private static boolean isFirstRun = true;

	@Test
	// This test is used to verify that getApplications is called the correct number of
	// times when a refresh event is fired. The getApplications call in
	// EurekaClientConfigurationRefresher.onApplicationEvent
	// ensures that the EurekaClient bean is recreated after a refresh event and that we
	// reregister the client with the server
	void verifyGetApplications() {
		if (publisher != null && isFirstRun) {
			publisher.publishEvent(new RefreshScopeRefreshedEvent());
			isFirstRun = false;
		}
		verify(client, times(3)).getApplications();
	}

}
