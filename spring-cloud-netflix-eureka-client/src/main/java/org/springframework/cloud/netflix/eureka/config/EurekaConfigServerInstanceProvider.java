/*
 * Copyright 2013-2022 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.Applications;
import com.netflix.discovery.shared.transport.EurekaHttpClient;
import com.netflix.discovery.shared.transport.EurekaHttpResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;
import org.springframework.http.HttpStatus;

public class EurekaConfigServerInstanceProvider {

	private final Log log;

	private final EurekaHttpClient client;

	private final EurekaClientConfig config;

	public EurekaConfigServerInstanceProvider(EurekaHttpClient client, EurekaClientConfig config) {
		this(LogFactory.getLog(EurekaConfigServerInstanceProvider.class), client, config);
	}

	public EurekaConfigServerInstanceProvider(Log log, EurekaHttpClient client, EurekaClientConfig config) {
		this.log = log;
		this.client = client;
		this.config = config;
	}

	public List<ServiceInstance> getInstances(String serviceId) {
		if (log.isDebugEnabled()) {
			log.debug("eurekaConfigServerInstanceProvider finding instances for " + serviceId);
		}
		EurekaHttpResponse<Applications> response = client.getApplications(config.getRegion());
		List<ServiceInstance> instances = new ArrayList<>();
		if (!isSuccessful(response) || response.getEntity() == null) {
			return instances;
		}

		Applications applications = response.getEntity();
		applications.shuffleInstances(config.shouldFilterOnlyUpInstances());
		List<InstanceInfo> infos = applications.getInstancesByVirtualHostName(serviceId);
		for (InstanceInfo info : infos) {
			instances.add(new EurekaServiceInstance(info));
		}
		if (log.isDebugEnabled()) {
			log.debug("eurekaConfigServerInstanceProvider found " + infos.size() + " instance(s) for " + serviceId
					+ ", " + instances);
		}
		return instances;
	}

	private boolean isSuccessful(EurekaHttpResponse<Applications> response) {
		HttpStatus httpStatus = HttpStatus.resolve(response.getStatusCode());
		return httpStatus != null && httpStatus.is2xxSuccessful();
	}

}
