/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.cloud.netflix.eureka;

import java.util.Map;

import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.LeaseInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * See com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider
 * @author Spencer Gibb
 */
public class InstanceInfoFactory {

	private static final Log log = LogFactory.getLog(InstanceInfoFactory.class);

	public InstanceInfo create(EurekaInstanceConfig config) {
		LeaseInfo.Builder leaseInfoBuilder = LeaseInfo.Builder.newBuilder()
				.setRenewalIntervalInSecs(config.getLeaseRenewalIntervalInSeconds())
				.setDurationInSecs(config.getLeaseExpirationDurationInSeconds());

		// Builder the instance information to be registered with eureka
		// server
		InstanceInfo.Builder builder = InstanceInfo.Builder.newBuilder();

		String namespace = config.getNamespace();
		if (!namespace.endsWith(".")) {
			namespace = namespace + ".";
		}
		builder.setNamespace(namespace).setAppName(config.getAppname())
				.setInstanceId(config.getInstanceId())
				.setAppGroupName(config.getAppGroupName())
				.setDataCenterInfo(config.getDataCenterInfo())
				.setIPAddr(config.getIpAddress()).setHostName(config.getHostName(false))
				.setPort(config.getNonSecurePort())
				.enablePort(InstanceInfo.PortType.UNSECURE,
						config.isNonSecurePortEnabled())
				.setSecurePort(config.getSecurePort())
				.enablePort(InstanceInfo.PortType.SECURE, config.getSecurePortEnabled())
				.setVIPAddress(config.getVirtualHostName())
				.setSecureVIPAddress(config.getSecureVirtualHostName())
				.setHomePageUrl(config.getHomePageUrlPath(), config.getHomePageUrl())
				.setStatusPageUrl(config.getStatusPageUrlPath(),
						config.getStatusPageUrl())
				.setHealthCheckUrls(config.getHealthCheckUrlPath(),
						config.getHealthCheckUrl(), config.getSecureHealthCheckUrl())
				.setASGName(config.getASGName());

		// Start off with the STARTING state to avoid traffic
		if (!config.isInstanceEnabledOnit()) {
			InstanceInfo.InstanceStatus initialStatus = InstanceInfo.InstanceStatus.STARTING;
			if (log.isInfoEnabled()) {
				log.info("Setting initial instance status as: " + initialStatus);
			}
			builder.setStatus(initialStatus);
		}
		else {
			if (log.isInfoEnabled()) {
				log.info("Setting initial instance status as: "
						+ InstanceInfo.InstanceStatus.UP
						+ ". This may be too early for the instance to advertise itself as available. "
						+ "You would instead want to control this via a healthcheck handler.");
			}
		}

		// Add any user-specific metadata information
		for (Map.Entry<String, String> mapEntry : config.getMetadataMap().entrySet()) {
			String key = mapEntry.getKey();
			String value = mapEntry.getValue();
			builder.add(key, value);
		}

		InstanceInfo instanceInfo = builder.build();
		instanceInfo.setLeaseInfo(leaseInfoBuilder.build());
		return instanceInfo;
	}
}
