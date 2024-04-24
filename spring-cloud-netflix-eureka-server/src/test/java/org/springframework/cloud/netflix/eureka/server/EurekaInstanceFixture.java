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

package org.springframework.cloud.netflix.eureka.server;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.LeaseInfo;

public final class EurekaInstanceFixture {

	private EurekaInstanceFixture() {
	}

	public static LeaseInfo getLeaseInfo() {
		LeaseInfo.Builder leaseBuilder = LeaseInfo.Builder.newBuilder();
		leaseBuilder.setRenewalIntervalInSecs(10);
		leaseBuilder.setDurationInSecs(15);
		return leaseBuilder.build();
	}

	public static InstanceInfo getInstanceInfo(String appName, String hostName, String instanceId, int port,
			LeaseInfo leaseInfo) {
		InstanceInfo.Builder builder = InstanceInfo.Builder.newBuilder();
		builder.setAppName(appName);
		builder.setHostName(hostName);
		builder.setInstanceId(instanceId);
		builder.setPort(port);
		builder.setLeaseInfo(leaseInfo);
		return builder.build();
	}

}
