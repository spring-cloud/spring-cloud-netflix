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
import java.util.Map;

import com.netflix.appinfo.AmazonInfo;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import com.netflix.turbine.discovery.Instance;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
public class EurekaInstanceDiscovery extends CommonsInstanceDiscovery {

	private static final Log log = LogFactory.getLog(EurekaInstanceDiscovery.class);

	private static final String EUREKA_DEFAULT_CLUSTER_NAME_EXPRESSION = "appName";

	private static final String ASG_KEY = "asg";

	private final EurekaClient eurekaClient;

	public EurekaInstanceDiscovery(TurbineProperties turbineProperties,
			EurekaClient eurekaClient) {
		super(turbineProperties, EUREKA_DEFAULT_CLUSTER_NAME_EXPRESSION);
		this.eurekaClient = eurekaClient;
	}

	/**
	 * Private helper that fetches the Instances for each application.
	 * @param serviceId of the service that the instance list should be returned for
	 * @return List of instances for a given service id
	 * @throws Exception - retrieving and marshalling service instances may result in an
	 * Exception
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
	 * @param instanceInfo {@link InstanceInfo} to marshal
	 * @return {@link Instance} marshaled from provided {@link InstanceInfo}
	 */
	Instance marshall(InstanceInfo instanceInfo) {
		String hostname = instanceInfo.getHostName();
		final String managementPort = instanceInfo.getMetadata().get("management.port");
		String port = managementPort == null ? String.valueOf(instanceInfo.getPort())
				: managementPort;
		String cluster = getClusterName(instanceInfo);
		Boolean status = parseInstanceStatus(instanceInfo.getStatus());
		if (hostname != null && cluster != null && status != null) {
			Instance instance = getInstance(hostname, port, cluster, status);

			Map<String, String> metadata = instanceInfo.getMetadata();
			boolean securePortEnabled = instanceInfo
					.isPortEnabled(InstanceInfo.PortType.SECURE);
			String securePort = String.valueOf(instanceInfo.getSecurePort());

			addMetadata(instance, hostname, port, securePortEnabled, securePort,
					metadata);

			// add amazon metadata
			String asgName = instanceInfo.getASGName();
			if (asgName != null) {
				instance.getAttributes().put(ASG_KEY, asgName);
			}

			DataCenterInfo dcInfo = instanceInfo.getDataCenterInfo();
			if (dcInfo != null && dcInfo.getName().equals(DataCenterInfo.Name.Amazon)) {
				AmazonInfo amznInfo = (AmazonInfo) dcInfo;
				instance.getAttributes().putAll(amznInfo.getMetadata());
			}

			return instance;
		}
		else {
			return null;
		}
	}

	/**
	 * Helper that returns whether the instance is Up of Down.
	 * @param status {@link InstanceStatus} instance to evaluate the status from
	 * @return {@code true} if {@link InstanceStatus} is UP
	 */
	protected Boolean parseInstanceStatus(InstanceStatus status) {
		if (status == null) {
			return null;
		}
		return status == InstanceStatus.UP;
	}

}
