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

package org.springframework.cloud.netflix.turbine;

import java.util.ArrayList;
import java.util.List;

import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides clusters names for Turbine based on applications names registered in Eureka.
 *
 * @author Anastasiia Smirnova
 */
public class EurekaBasedTurbineClustersProvider implements TurbineClustersProvider {

	private static final Log log = LogFactory
			.getLog(EurekaBasedTurbineClustersProvider.class);

	private final EurekaClient eurekaClient;

	public EurekaBasedTurbineClustersProvider(EurekaClient eurekaClient) {
		this.eurekaClient = eurekaClient;
	}

	@Override
	public List<String> getClusterNames() {
		Applications applications = eurekaClient.getApplications();
		List<Application> registeredApplications = applications
				.getRegisteredApplications();
		List<String> appNames = new ArrayList<>(registeredApplications.size());
		for (Application application : registeredApplications) {
			appNames.add(application.getName());
		}
		log.trace("Using clusters names: " + appNames);
		return appNames;
	}

}
