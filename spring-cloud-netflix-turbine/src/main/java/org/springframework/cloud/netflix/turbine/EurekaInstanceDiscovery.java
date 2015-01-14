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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.netflix.appinfo.AmazonInfo;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.discovery.DiscoveryManager;
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
 *
 * All the logic to perform this translation can be overriden here, so that you can
 * provide your own implementation if needed.
 */
public class EurekaInstanceDiscovery implements InstanceDiscovery {

	private static final Logger logger = LoggerFactory
			.getLogger(EurekaInstanceDiscovery.class);

	// Property the controls the list of applications that are enabled in Eureka
	private static final DynamicStringProperty ApplicationList = DynamicPropertyFactory
			.getInstance().getStringProperty("turbine.appConfig", "");

	private final Expression clusterNameExpression;

	public EurekaInstanceDiscovery(TurbineProperties turbineProperties) {
		// Eureka client should already be configured by spring-platform-netflix-core
		// initialize eureka client.
		// DiscoveryManager.getInstance().initComponent(new MyDataCenterInstanceConfig(),
		// new DefaultEurekaClientConfig());

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

		List<Instance> instances = new ArrayList<Instance>();

		List<String> appNames = parseApps();
		if (appNames == null || appNames.size() == 0) {
			logger.info("No apps configured, returning an empty instance list");
			return instances;
		}

		logger.info("Fetching instance list for apps: " + appNames);

		for (String appName : appNames) {
			try {
				instances.addAll(getInstancesForApp(appName));
			}
			catch (Exception e) {
				logger.error("Failed to fetch instances for app: " + appName
						+ ", retrying once more", e);
				try {
					instances.addAll(getInstancesForApp(appName));
				}
				catch (Exception e1) {
					logger.error("Failed again to fetch instances for app: " + appName
							+ ", giving up", e);
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

		List<Instance> instances = new ArrayList<Instance>();

		logger.info("Fetching instances for app: {}", appName);
		Application app = DiscoveryManager.getInstance().getDiscoveryClient()
				.getApplication(appName);
		if (app == null) {
			logger.warn("Eureka returned null for app: {}", appName);
		}
		List<InstanceInfo> instancesForApp = app.getInstances();

		if (instancesForApp != null) {
			logger.info("Received instance list for app: {} = {}", appName,
					instancesForApp.size());
			for (InstanceInfo iInfo : instancesForApp) {
				Instance instance = marshallInstanceInfo(iInfo);
				if (instance != null) {
					instances.add(instance);
				}
			}
		}

		return instances;
	}

	/**
	 * Private helper that marshals the information from each instance into something that
	 * Turbine can understand. Override this method for your own implementation for
	 * parsing Eureka info.
	 *
	 * @param iInfo
	 * @return Instance
	 */
	protected Instance marshallInstanceInfo(InstanceInfo iInfo) {

		String hostname = iInfo.getHostName();
		String cluster = getClusterName(iInfo);
		Boolean status = parseInstanceStatus(iInfo.getStatus());

		if (hostname != null && cluster != null && status != null) {
			Instance instance = new Instance(hostname, cluster, status);
			Map<String, String> metadata = iInfo.getMetadata();
			if (metadata != null) {
				instance.getAttributes().putAll(metadata);
			}

			String asgName = iInfo.getASGName();
			if (asgName != null) {
				instance.getAttributes().put("asg", asgName);
			}
			instance.getAttributes().put("port", String.valueOf(iInfo.getPort()));

			DataCenterInfo dcInfo = iInfo.getDataCenterInfo();
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
	 * Helper that returns whether the instance is Up of Down
	 * @param status
	 * @return
	 */
	protected Boolean parseInstanceStatus(InstanceStatus status) {

		if (status != null) {
			if (status == InstanceStatus.UP) {
				return Boolean.TRUE;
			}
			else {
				return Boolean.FALSE;
			}
		}
		else {
			return null;
		}
	}

	/**
	 * Helper that fetches the cluster name. Cluster is a Turbine concept and not a Eureka
	 * concept. By default we choose the amazon asg name as the cluster. A custom
	 * implementation can be plugged in by overriding this method.
	 *
	 * @param iInfo
	 * @return
	 */
	protected String getClusterName(InstanceInfo iInfo) {
		StandardEvaluationContext context = new StandardEvaluationContext(iInfo);
		Object value = this.clusterNameExpression.getValue(context);
		if (value != null) {
			return value.toString();
		}
		return null;
	}

	/**
	 * TODO: move to ConfigurationProperties Private helper that parses the list of
	 * application names.
	 *
	 * @return List<String>
	 */
	private List<String> parseApps() {

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
