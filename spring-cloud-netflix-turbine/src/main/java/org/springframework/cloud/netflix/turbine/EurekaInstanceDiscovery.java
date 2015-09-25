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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.netflix.discovery.EurekaClient;
import lombok.extern.apachecommons.CommonsLog;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.netflix.appinfo.AmazonInfo;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.discovery.shared.Application;
import com.netflix.turbine.discovery.Instance;
import com.netflix.turbine.discovery.InstanceDiscovery;

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
public class EurekaInstanceDiscovery implements InstanceDiscovery {

	// Property the controls the list of applications that are enabled in Eureka
	private static final DynamicStringProperty ApplicationList = DynamicPropertyFactory
			.getInstance().getStringProperty("turbine.appConfig", "");

	private final EurekaClient eurekaClient;
	private final Expression clusterNameExpression;

	public EurekaInstanceDiscovery(TurbineProperties turbineProperties, EurekaClient eurekaClient) {
		this.eurekaClient = eurekaClient;
		SpelExpressionParser parser = new SpelExpressionParser();
		this.clusterNameExpression = parser.parseExpression(turbineProperties
				.getClusterNameExpression());
	}

	/**
	 * Method that queries Eureka service for a list of configured application names
	 * @return Collection<Instance>
	 */
	@Override
	public Collection<Instance> getInstanceList() throws Exception {
		List<Instance> instances = new ArrayList<>();
		List<String> appNames = parseApps();
		if (appNames == null || appNames.size() == 0) {
			log.info("No apps configured, returning an empty instance list");
			return instances;
		}
		log.info("Fetching instance list for apps: " + appNames);
		for (String appName : appNames) {
			try {
				instances.addAll(getInstancesForApp(appName));
			}
			catch (Exception ex) {
				log.error("Failed to fetch instances for app: " + appName
						+ ", retrying once more", ex);
				try {
					instances.addAll(getInstancesForApp(appName));
				}
				catch (Exception retryException) {
					log.error("Failed again to fetch instances for app: " + appName
							+ ", giving up", ex);
				}
			}
		}
		return instances;
	}

	/**
	 * Private helper that fetches the Instances for each application.
	 * @param appName
	 * @return List<Instance>
	 * @throws Exception
	 */
	private List<Instance> getInstancesForApp(String appName) throws Exception {
		List<Instance> instances = new ArrayList<>();
		log.info("Fetching instances for app: " + appName);
		Application app = eurekaClient.getApplication(appName);
		if (app == null) {
			log.warn("Eureka returned null for app: " + appName);
		}
		try {
			List<InstanceInfo> instancesForApp = app.getInstances();
			if (instancesForApp != null) {
				log.info("Received instance list for app: " + appName + ", size="
						+ instancesForApp.size());
				for (InstanceInfo iInfo : instancesForApp) {
					Instance instance = marshallInstanceInfo(iInfo);
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
	protected Instance marshallInstanceInfo(InstanceInfo instanceInfo) {
		String hostname = instanceInfo.getHostName();
		String cluster = getClusterName(instanceInfo);
		Boolean status = parseInstanceStatus(instanceInfo.getStatus());
		if (hostname != null && cluster != null && status != null) {
			Instance instance = new Instance(hostname, cluster, status);

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

	/**
	 * Helper that fetches the cluster name. Cluster is a Turbine concept and not a Eureka
	 * concept. By default we choose the amazon asg name as the cluster. A custom
	 * implementation can be plugged in by overriding this method.
	 */
	protected String getClusterName(InstanceInfo iInfo) {
		StandardEvaluationContext context = new StandardEvaluationContext(iInfo);
		Object value = this.clusterNameExpression.getValue(context);
		if (value != null) {
			return value.toString();
		}
		return null;
	}

	private List<String> parseApps() {
		// TODO: move to ConfigurationProperties Private helper that parses the list of
		// application names.
		String appList = ApplicationList.get();
		if (appList == null) {
			return null;
		}
		appList = appList.trim();
		if (appList.length() == 0) {
			return null;
		}
		String[] parts = appList.split(",");
		if (parts != null && parts.length > 0) {
			return Arrays.asList(parts);
		}
		return null;
	}

}
