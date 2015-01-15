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

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;

/**
 * @author Spencer Gibb
 */
public class EurekaDiscoveryClient implements DiscoveryClient {

	public static final String DESCRIPTION = "Spring Cloud Eureka Discovery Client";

	@Autowired
	private EurekaInstanceConfigBean config;

	@Autowired
	private com.netflix.discovery.DiscoveryClient discovery;

	@Override
	public String description() {
		return DESCRIPTION;
	}

	@Override
	public ServiceInstance getLocalServiceInstance() {
		return new ServiceInstance() {
			@Override
			public String getServiceId() {
				return EurekaDiscoveryClient.this.config.getAppname();
			}

			@Override
			public String getHost() {
				return EurekaDiscoveryClient.this.config.getHostname();
			}

			@Override
			public int getPort() {
				return EurekaDiscoveryClient.this.config.getNonSecurePort();
			}
		};
	}

	@Override
	public List<ServiceInstance> getInstances(String serviceId) {
		List<InstanceInfo> infos = this.discovery.getInstancesByVipAddress(serviceId,
				false);
		Iterable<ServiceInstance> instances = transform(infos,
				new Function<InstanceInfo, ServiceInstance>() {
					@Nullable
					@Override
					public ServiceInstance apply(@Nullable InstanceInfo info) {
						return new EurekaServiceInstance(info);
					}
				});
		return Lists.newArrayList(instances);
	}

	static class EurekaServiceInstance implements ServiceInstance {
		private InstanceInfo instance;

		EurekaServiceInstance(InstanceInfo instance) {
			this.instance = instance;
		}

		@Override
		public String getServiceId() {
			return this.instance.getAppName();
		}

		@Override
		public String getHost() {
			return this.instance.getHostName();
		}

		@Override
		public int getPort() {
			return this.instance.getPort();
		}
	}

	@Override
	public List<String> getServices() {
		Applications applications = this.discovery.getApplications();
		if (applications == null) {
			return Collections.emptyList();
		}
		return Lists.newArrayList(filter(
				transform(applications.getRegisteredApplications(),
						new Function<Application, String>() {
							@Nullable
							@Override
							public String apply(@Nullable Application app) {
								if (app.getInstances().isEmpty()) {
									return null;
								}
								return app.getName().toLowerCase();
							}
						}), Predicates.notNull()));
	}

	@Override
	public List<ServiceInstance> getAllInstances() {
		Applications applications = this.discovery.getApplications();
		if (applications == null) {
			return Collections.emptyList();
		}
		Iterable<ServiceInstance> instances = transform(
				concat(transform(applications.getRegisteredApplications(),
						new Function<Application, List<InstanceInfo>>() {
							@Override
							public List<InstanceInfo> apply(@Nullable Application app) {
								return app.getInstances();
							}
						})), new Function<InstanceInfo, ServiceInstance>() {
					@Nullable
					@Override
					public ServiceInstance apply(@Nullable InstanceInfo info) {
						return new EurekaServiceInstance(info);
					}
				});
		return Lists.newArrayList(instances);
	}

}
