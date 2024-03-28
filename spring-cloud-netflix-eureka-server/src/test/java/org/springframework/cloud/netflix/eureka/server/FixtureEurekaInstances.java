package org.springframework.cloud.netflix.eureka.server;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.LeaseInfo;

public final class FixtureEurekaInstances {

	private FixtureEurekaInstances() {
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
