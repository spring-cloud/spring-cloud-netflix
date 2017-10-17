/*
 * Copyright 2013-2017 the original author or authors.
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
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.netflix.turbine.discovery.Instance;
import com.netflix.turbine.discovery.InstanceDiscovery;

/**
 * Class that encapsulates an {@link InstanceDiscovery}
 * implementation that uses Spring Cloud Commons (see https://github.com/spring-cloud/spring-cloud-commons)
 * The plugin requires a list of applications configured. It then queries the set of
 * instances for * each application. Instance information retrieved from the {@link DiscoveryClient}
 * must be translated to * something that Turbine can understand i.e the
 * {@link Instance} class.
 * <p>
 * All the logic to perform this translation can be overriden here, so that you can
 * provide your own implementation if needed.
 *
 * @author Spencer Gibb
 */
public class CommonsInstanceDiscovery implements InstanceDiscovery {

	private static final Log log = LogFactory.getLog(CommonsInstanceDiscovery.class);

	private static final String DEFAULT_CLUSTER_NAME_EXPRESSION = "serviceId";
	protected static final String PORT_KEY = "port";
	protected static final String SECURE_PORT_KEY = "securePort";
	protected static final String FUSED_HOST_PORT_KEY = "fusedHostPort";

	private final Expression clusterNameExpression;
	private DiscoveryClient discoveryClient;
	private TurbineProperties turbineProperties;
	private final boolean combineHostPort;

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
		this.combineHostPort = turbineProperties.isCombineHostPort();
	}

	protected Expression getClusterNameExpression() {
		return clusterNameExpression;
	}

	public TurbineProperties getTurbineProperties() {
		return turbineProperties;
	}

	protected boolean isCombineHostPort() {
		return combineHostPort;
	}

	/**
	 * Method that queries DiscoveryClient for a list of configured application names
	 * @return Collection<Instance>
	 */
	@Override
	public Collection<Instance> getInstanceList() throws Exception {
		List<Instance> instances = new ArrayList<>();
		List<String> appNames = getApplications();
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

	protected List<String> getApplications() {
		return turbineProperties.getAppConfigList();
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
	 * Turbine can understand. Override this method for your own implementation.
	 * @param serviceInstance
	 * @return Instance
	 */
	Instance marshall(ServiceInstance serviceInstance) {
		String hostname = serviceInstance.getHost();
			String managementPort = serviceInstance.getMetadata().get("management.port");
			String port = managementPort == null ? String.valueOf(serviceInstance.getPort()) : managementPort;
		String cluster = getClusterName(serviceInstance);
		Boolean status = Boolean.TRUE; //TODO: where to get?
		if (hostname != null && cluster != null && status != null) {
			Instance instance = getInstance(hostname, port, cluster, status);

			Map<String, String> metadata = serviceInstance.getMetadata();
			boolean securePortEnabled = serviceInstance.isSecure();

			addMetadata(instance, hostname, port, securePortEnabled, port, metadata);

			return instance;
		}
		else {
			return null;
		}
	}

	protected void addMetadata(Instance instance, String hostname, String port, boolean securePortEnabled, String securePort, Map<String, String> metadata) {
		// add metadata
		if (metadata != null) {
			instance.getAttributes().putAll(metadata);
		}

		// add ports
		instance.getAttributes().put(PORT_KEY, port);
		if (securePortEnabled) {
			instance.getAttributes().put(SECURE_PORT_KEY, securePort);
		}
		if (this.isCombineHostPort()) {
			String fusedHostPort = securePortEnabled ? hostname+":"+securePort : instance.getHostname() ;
			instance.getAttributes().put(FUSED_HOST_PORT_KEY, fusedHostPort);
		}
	}

	protected Instance getInstance(String hostname, String port, String cluster, Boolean status) {
		String hostPart = this.isCombineHostPort() ? hostname+":"+port : hostname;
		return new Instance(hostPart, cluster, status);
	}

	/**
	 * Helper that fetches the cluster name. Cluster is a Turbine concept and not a commons
	 * concept. By default we choose the amazon serviceId as the cluster. A custom
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
