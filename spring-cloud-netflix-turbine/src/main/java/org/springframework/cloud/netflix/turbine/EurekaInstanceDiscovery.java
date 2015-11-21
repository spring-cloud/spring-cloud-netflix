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

package org.springframework.cloud.netflix.turbine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.netflix.appinfo.AmazonInfo;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import com.netflix.turbine.discovery.Instance;

import lombok.extern.apachecommons.CommonsLog;

/**
 * Class that encapsulates an {@link com.netflix.turbine.discovery.InstanceDiscovery}
 * implementation that uses Eureka (see https://github.com/Netflix/eureka) The plugin
 * requires a list of applications configured. It then queries the set of instances for
 * each application. Instance information retrieved from Eureka must be translated to
 * something that Turbine can understand i.e the
 * {@link com.netflix.turbine.discovery.Instance} class.
 * <p>
 * All the logic to perform this translation can be overriden here, so that you can
 * provide your own implementation if needed.
 *
 * @author Spencer Gibb
 */
@CommonsLog
public class EurekaInstanceDiscovery extends CommonsInstanceDiscovery {

	private static final String EUREKA_DEFAULT_CLUSTER_NAME_EXPRESSION = "appName";

	private final EurekaClient eurekaClient;
	private final boolean combineHostPort;

	public EurekaInstanceDiscovery(TurbineProperties turbineProperties, EurekaClient eurekaClient) {
		super(turbineProperties, EUREKA_DEFAULT_CLUSTER_NAME_EXPRESSION);
		this.eurekaClient = eurekaClient;
		this.combineHostPort = turbineProperties.isCombineHostPort();
	}

	/**
	 * Private helper that fetches the Instances for each application.
	 * @param serviceId
	 * @return List<Instance>
	 * @throws Exception
	 */
	@Override
	protected List<Instance> getInstancesForApp(String serviceId) throws Exception {
		List<Instance> instances = new ArrayList<>();
		log.info("Fetching instances for app: " + serviceId);
		Application app = eurekaClient.getApplication(serviceId);
		if (app == null) {
			log.warn("Eureka returned null for app: " + serviceId);
			return instances;
		}
		try {
			List<InstanceInfo> instancesForApp = app.getInstances();
			if (instancesForApp != null) {
				log.info("Received instance list for app: " + serviceId + ", size="
						+ instancesForApp.size());
				for (InstanceInfo iInfo : instancesForApp) {
					Instance instance = marshall(iInfo);
					if (instance != null) {
						instances.add(instance);
					}
				}
			}
		}
		catch (Exception e) {
			log.warn("Failed to retrieve instances from Eureka", e);
		}
		return instances;
	}

	/**
	 * Private helper that marshals the information from each instance into something that
	 * Turbine can understand. Override this method for your own implementation for
	 * parsing Eureka info.
	 * @param instanceInfo
	 * @return Instance
	 */
	Instance marshall(InstanceInfo instanceInfo) {
		String hostname = instanceInfo.getHostName();
		String port = String.valueOf(instanceInfo.getPort());
		String cluster = getClusterName(instanceInfo);
		Boolean status = parseInstanceStatus(instanceInfo.getStatus());
		if (hostname != null && cluster != null && status != null) {
			String hostPart = combineHostPort ? hostname+":"+port : hostname;
			Instance instance = new Instance(hostPart, cluster, status);

			// add metadata
			Map<String, String> metadata = instanceInfo.getMetadata();
			if (metadata != null) {
				instance.getAttributes().putAll(metadata);
			}

			// add amazon metadata
			String asgName = instanceInfo.getASGName();
			if (asgName != null) {
				instance.getAttributes().put("asg", asgName);
			}
			DataCenterInfo dcInfo = instanceInfo.getDataCenterInfo();
			if (dcInfo != null && dcInfo.getName().equals(DataCenterInfo.Name.Amazon)) {
				AmazonInfo amznInfo = (AmazonInfo) dcInfo;
				instance.getAttributes().putAll(amznInfo.getMetadata());
			}

			// add ports
			instance.getAttributes().put("port", String.valueOf(instanceInfo.getPort()));
			boolean securePortEnabled = instanceInfo.isPortEnabled(InstanceInfo.PortType.SECURE);
			if (securePortEnabled) {
				instance.getAttributes().put("securePort", String.valueOf(instanceInfo.getSecurePort()));
			}

			if (combineHostPort) {
				String fusedHostPort = securePortEnabled ? hostname+":"+String.valueOf(instanceInfo.getSecurePort()) : hostPart ;
				instance.getAttributes().put("fusedHostPort", fusedHostPort);
			}
			return instance;
		}
		else {
			return null;
		}
	}

	/**
	 * Helper that returns whether the instance is Up of Down
	 */
	protected Boolean parseInstanceStatus(InstanceStatus status) {
		if (status == null) {
			return null;
		}
		return status == InstanceStatus.UP;
	}


}
