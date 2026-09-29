/*
 * Copyright 2013-present the original author or authors.
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

import com.netflix.appinfo.InstanceInfo.InstanceStatus;

import org.springframework.cloud.context.restart.PauseHandler;

/**
 * {@link PauseHandler} that takes the instance out of service in Eureka when the
 * application is paused and cancels that status override when it is resumed.
 *
 * @author Akhil CH
 */
public class EurekaPauseHandler implements PauseHandler {

	private final EurekaServiceRegistry serviceRegistry;

	private final EurekaRegistration registration;

	public EurekaPauseHandler(EurekaServiceRegistry serviceRegistry, EurekaRegistration registration) {
		this.serviceRegistry = serviceRegistry;
		this.registration = registration;
	}

	@Override
	public void pause() {
		this.serviceRegistry.setStatus(this.registration, InstanceStatus.OUT_OF_SERVICE.name());
	}

	@Override
	public void resume() {
		this.serviceRegistry.setStatus(this.registration, "CANCEL_OVERRIDE");
	}

}
