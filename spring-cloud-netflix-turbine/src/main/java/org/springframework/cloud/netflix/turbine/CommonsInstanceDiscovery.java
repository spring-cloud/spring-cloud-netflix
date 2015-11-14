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
import java.util.Collection;
import java.util.List;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.netflix.turbine.discovery.Instance;
import com.netflix.turbine.discovery.InstanceDiscovery;

import lombok.extern.apachecommons.CommonsLog;

/**
 * Class that encapsulates an {@link InstanceDiscovery}
 * implementation that uses Eureka (see https://github.com/Netflix/eureka) The plugin
 * requires a list of applications configured. It then queries the set of instances for
 * each application. Instance information retrieved from Eureka must be translated to
 * something that Turbine can understand i.e the
 * {@link Instance} class.
 * <p>
 * All the logic to perform this translation can be overriden here, so that you can
 * provide your own implementation if needed.
 *
 * @author Spencer Gibb
 */
@CommonsLog
public class CommonsInstanceDiscovery implements InstanceDiscovery {

	private static final String DEFAULT_CLUSTER_NAME_EXPRESSION = "serviceId";

	private final Expression clusterNameExpression;
	private DiscoveryClient discoveryClient;
	private TurbineProperties turbineProperties;

	public CommonsInstanceDiscovery(TurbineProperties turbineProperties, DiscoveryClient discoveryClient) {
		this(turbineProperties, DEFAULT_CLUSTER_NAME_EXPRESSION);
		this.discoveryClient = discoveryClient;
	}

	protected CommonsInstanceDiscovery(TurbineProperties turbineProperties, String defaultExpression) {
		this.turbineProperties = turbineProperties;
		SpelExpressionParser parser = new SpelExpressionParser();
		String clusterNameExpression = turbineProperties
				.getClusterNameExpression();
		if (clusterNameExpression == null) {
			clusterNameExpression = defaultExpression;
		}
		this.clusterNameExpression = parser.parseExpression(clusterNameExpression);
	}

	protected Expression getClusterNameExpression() {
		return clusterNameExpression;
	}

	public TurbineProperties getTurbineProperties() {
		return turbineProperties;
	}

	/**
	 * Method that queries Eureka service for a list of configured application names
	 * @return Collection<Instance>
	 */
	@Override
	public Collection<Instance> getInstanceList() throws Exception {
		List<Instance> instances = new ArrayList<>();
		List<String> appNames = getTurbineProperties().getAppConfigList();
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
	 * helper that fetches the Instances for each application from DiscoveryClient.
	 * @param serviceId
	 * @return List<Instance>
	 * @throws Exception
	 */
	protected List<Instance> getInstancesForApp(String serviceId) throws Exception {
		List<Instance> instances = new ArrayList<>();
		log.info("Fetching instances for app: " + serviceId);
		List<ServiceInstance> serviceInstances = discoveryClient.getInstances(serviceId);
		if (serviceInstances == null || serviceInstances.isEmpty()) {
			log.warn("DiscoveryClient returned null or empty for service: " + serviceId);
			return instances;
		}
		try {
			log.info("Received instance list for service: " + serviceId + ", size="
					+ serviceInstances.size());
			for (ServiceInstance serviceInstance : serviceInstances) {
				Instance instance = marshall(serviceInstance);
				if (instance != null) {
					instances.add(instance);
				}
			}
		}
		catch (Exception e) {
			log.warn("Failed to retrieve instances from DiscoveryClient", e);
		}
		return instances;
	}

	/**
	 * Private helper that marshals the information from each instance into something that
	 * Turbine can understand. Override this method for your own implementation for
	 * parsing Eureka info.
	 * @param serviceInstance
	 * @return Instance
	 */
	private Instance marshall(ServiceInstance serviceInstance) {
		String hostname = serviceInstance.getHost();
		String cluster = getClusterName(serviceInstance);
		Boolean status = Boolean.TRUE; //TODO: where to get?
		if (hostname != null && cluster != null && status != null) {
			Instance instance = new Instance(hostname, cluster, status);

			// TODO: reimplement when metadata is in commons
			// add metadata
			/*Map<String, String> metadata = instanceInfo.getMetadata();
			if (metadata != null) {
				instance.getAttributes().putAll(metadata);
			}*/

			// add ports
			instance.getAttributes().put("port", String.valueOf(serviceInstance.getPort()));
			boolean securePortEnabled = serviceInstance.isSecure();
			if (securePortEnabled) {
				instance.getAttributes().put("securePort", String.valueOf(serviceInstance.getPort()));
			}
			return instance;
		}
		else {
			return null;
		}
	}

	/**
	 * Helper that fetches the cluster name. Cluster is a Turbine concept and not a Eureka
	 * concept. By default we choose the amazon asg name as the cluster. A custom
	 * implementation can be plugged in by overriding this method.
	 */
	protected String getClusterName(Object object) {
		StandardEvaluationContext context = new StandardEvaluationContext(object);
		Object value = this.clusterNameExpression.getValue(context);
		if (value != null) {
			return value.toString();
		}
		return null;
	}

}
