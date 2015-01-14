/*
 * Copyright 2013-2014 the original author or authors.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.cloud.client.discovery.DiscoveryHealthIndicator;

import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;

/**
 * @author Dave Syer
 *
 */
public class EurekaHealthIndicator implements DiscoveryHealthIndicator {

	private EurekaInstanceConfig instanceConfig;

	private MetricReader metrics;

	private int failCount = 0;

	private DiscoveryClient discovery;

	public EurekaHealthIndicator(DiscoveryClient discovery, MetricReader metrics,
			EurekaInstanceConfig instanceConfig) {
		super();
		this.discovery = discovery;
		this.metrics = metrics;
		this.instanceConfig = instanceConfig;
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
		Status status = new Status(this.discovery.getInstanceRemoteStatus().toString(),
				"Remote status from Eureka server");
		@SuppressWarnings("unchecked")
		Metric<Number> value = (Metric<Number>) this.metrics
				.findOne("counter.servo.discoveryclient_failed");
		if (value != null) {
			int renewalPeriod = this.instanceConfig.getLeaseRenewalIntervalInSeconds();
			int latest = value.getValue().intValue();
			builder.withDetail("failCount", latest);
			builder.withDetail("renewalPeriod", renewalPeriod);
			if (this.failCount < latest) {
				status = new Status("UP", "Eureka discovery client is reporting failures");
				this.failCount = latest;
			}
			else {
				status = new Status("UP", "No new failures in Eureka discovery client");
			}
		}
		return status;
	}

	private Map<String, Object> getApplications() {
		Applications applications = this.discovery.getApplications();
		if (applications == null) {
			return Collections.emptyMap();
		}
		Map<String, Object> result = new HashMap<String, Object>();
		for (Application application : applications.getRegisteredApplications()) {
			result.put(application.getName(), application.getInstances().size());
		}
		return result;
	}

}
