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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.Ordered;

/**
 * A {@link DiscoveryClient} implementation for Eureka.
 *
 * @author Spencer Gibb
 * @author Tim Ysewyn
 */
public class EurekaDiscoveryClient implements DiscoveryClient {

	/**
	 * Client description {@link String}.
	 */
	public static final String DESCRIPTION = "Spring Cloud Eureka Discovery Client";

	private final EurekaClient eurekaClient;

	private final EurekaClientConfig clientConfig;

	@Deprecated
	public EurekaDiscoveryClient(EurekaInstanceConfig config, EurekaClient eurekaClient) {
		this(eurekaClient, eurekaClient.getEurekaClientConfig());
	}

	public EurekaDiscoveryClient(EurekaClient eurekaClient,
			EurekaClientConfig clientConfig) {
		this.clientConfig = clientConfig;
		this.eurekaClient = eurekaClient;
	}

	@Override
	public String description() {
		return DESCRIPTION;
	}

	@Override
	public List<ServiceInstance> getInstances(String serviceId) {
		List<InstanceInfo> infos = this.eurekaClient.getInstancesByVipAddress(serviceId,
				false);
		List<ServiceInstance> instances = new ArrayList<>();
		for (InstanceInfo info : infos) {
			instances.add(new EurekaServiceInstance(info));
		}
		return instances;
	}

	@Override
	public List<String> getServices() {
		Applications applications = this.eurekaClient.getApplications();
		if (applications == null) {
			return Collections.emptyList();
		}
		List<Application> registered = applications.getRegisteredApplications();
		List<String> names = new ArrayList<>();
		for (Application app : registered) {
			if (app.getInstances().isEmpty()) {
				continue;
			}
			names.add(app.getName().toLowerCase());

		}
		return names;
	}

	@Override
	public int getOrder() {
		return clientConfig instanceof Ordered ? ((Ordered) clientConfig).getOrder()
				: DiscoveryClient.DEFAULT_ORDER;
	}

	/**
	 * An Eureka-specific {@link ServiceInstance} implementation. Extends
	 * {@link org.springframework.cloud.netflix.eureka.EurekaServiceInstance} for
	 * backwards compatibility.
	 *
	 * @deprecated In favor of
	 * {@link org.springframework.cloud.netflix.eureka.EurekaServiceInstance}.
	 */
	@Deprecated
	public static class EurekaServiceInstance
			extends org.springframework.cloud.netflix.eureka.EurekaServiceInstance {

		public EurekaServiceInstance(InstanceInfo instance) {
			super(instance);
		}

	}

}
