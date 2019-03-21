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

package org.springframework.cloud.netflix.eureka;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;

import org.springframework.aop.support.AopUtils;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.discovery.health.DiscoveryHealthIndicator;
import org.springframework.cloud.util.ProxyUtils;

/**
 * @author Dave Syer
 */
public class EurekaHealthIndicator implements DiscoveryHealthIndicator {

	private final EurekaClient eurekaClient;

	private final EurekaInstanceConfig instanceConfig;

	private final EurekaClientConfig clientConfig;

	public EurekaHealthIndicator(EurekaClient eurekaClient,
			EurekaInstanceConfig instanceConfig, EurekaClientConfig clientConfig) {
		super();
		this.eurekaClient = eurekaClient;
		this.instanceConfig = instanceConfig;
		this.clientConfig = clientConfig;
	}

	@Override
	public String getName() {
		return "eureka";
	}

	@Override
	public Health health() {
		Builder builder = Health.unknown();
		Status status = getStatus(builder);
		return builder.status(status).withDetail("applications", getApplications())
				.build();
	}

	private Status getStatus(Builder builder) {
		Status status = new Status(this.eurekaClient.getInstanceRemoteStatus().toString(),
				"Remote status from Eureka server");
		DiscoveryClient discoveryClient = getDiscoveryClient();
		if (discoveryClient != null && clientConfig.shouldFetchRegistry()) {
			long lastFetch = discoveryClient.getLastSuccessfulRegistryFetchTimePeriod();

			if (lastFetch < 0) {
				status = new Status("UP",
						"Eureka discovery client has not yet successfully connected to a Eureka server");
			}
			else if (lastFetch > clientConfig.getRegistryFetchIntervalSeconds() * 2000) {
				status = new Status("UP",
						"Eureka discovery client is reporting failures to connect to a Eureka server");
				builder.withDetail("renewalPeriod",
						instanceConfig.getLeaseRenewalIntervalInSeconds());
				builder.withDetail("failCount",
						lastFetch / clientConfig.getRegistryFetchIntervalSeconds());
			}
		}

		return status;
	}

	private DiscoveryClient getDiscoveryClient() {
		DiscoveryClient discoveryClient = null;
		if (AopUtils.isAopProxy(eurekaClient)) {
			discoveryClient = ProxyUtils.getTargetObject(eurekaClient);
		}
		else if (DiscoveryClient.class.isInstance(eurekaClient)) {
			discoveryClient = (DiscoveryClient) eurekaClient;
		}
		return discoveryClient;
	}

	private Map<String, Object> getApplications() {
		Applications applications = this.eurekaClient.getApplications();
		if (applications == null) {
			return Collections.emptyMap();
		}
		Map<String, Object> result = new HashMap<>();
		for (Application application : applications.getRegisteredApplications()) {
			if (!application.getInstances().isEmpty()) {
				result.put(application.getName(), application.getInstances().size());
			}
		}
		return result;
	}

}
