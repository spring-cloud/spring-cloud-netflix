/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.sidecar;

import static com.netflix.appinfo.InstanceInfo.InstanceStatus.DOWN;
import static com.netflix.appinfo.InstanceInfo.InstanceStatus.OUT_OF_SERVICE;
import static com.netflix.appinfo.InstanceInfo.InstanceStatus.UNKNOWN;
import static com.netflix.appinfo.InstanceInfo.InstanceStatus.UP;

import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

/**
 * Eureka HealthCheckHandler that translates boot health status to
 * InstanceStatus so the proper status of the non-JVM app is sent to Eureka.
* @author Spencer Gibb
*/
class LocalApplicationHealthCheckHandler implements HealthCheckHandler {

	private final HealthIndicator healthIndicator;

	public LocalApplicationHealthCheckHandler(HealthIndicator healthIndicator) {
		this.healthIndicator = healthIndicator;
	}

	@Override
	public InstanceStatus getStatus(InstanceStatus currentStatus) {
		Status status = healthIndicator.health().getStatus();
		if (status.equals(Status.UP)) {
			return UP;
		} else if (status.equals(Status.OUT_OF_SERVICE)) {
			return OUT_OF_SERVICE;
		} else if (status.equals(Status.DOWN)) {
			return DOWN;
		}
		return UNKNOWN;
	}
}
