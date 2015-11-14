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

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.turbine.data.DataFromSingleInstance;
import com.netflix.turbine.discovery.Instance;
import com.netflix.turbine.handler.PerformanceCriteria;
import com.netflix.turbine.monitor.MonitorConsole;
import com.netflix.turbine.monitor.cluster.AggregateClusterMonitor;
import com.netflix.turbine.monitor.cluster.ObservationCriteria;
import com.netflix.turbine.monitor.instance.InstanceUrlClosure;

/**
 * @author Spencer Gibb
 */
public class SpringClusterMonitor extends AggregateClusterMonitor {

	// TODO: convert to ConfigurationProperties (how to do per-cluster configuration?

	public SpringClusterMonitor(String name, String clusterName) {
		super(name, new ObservationCriteria.ClusterBasedObservationCriteria(clusterName),
				new PerformanceCriteria.AggClusterPerformanceCriteria(clusterName),
				new MonitorConsole<DataFromSingleInstance>(), InstanceMonitorDispatcher,
				SpringClusterMonitor.ClusterConfigBasedUrlClosure);
	}

	/**
	 * TODO: make this a template of some kind (secure, management port, etc...) Helper
	 * class that decides how to connect to a server based on injected config. Note that
	 * the cluster name must be provided here since one can have different configs for
	 * different clusters
	 */
	public static InstanceUrlClosure ClusterConfigBasedUrlClosure = new InstanceUrlClosure() {

		private final DynamicStringProperty defaultUrlClosureConfig = DynamicPropertyFactory
				.getInstance().getStringProperty("turbine.instanceUrlSuffix",
						"hystrix.stream");
		private final DynamicBooleanProperty instanceInsertPort = DynamicPropertyFactory
				.getInstance().getBooleanProperty("turbine.instanceInsertPort", true);

		@Override
		public String getUrlPath(Instance host) {
			if (host.getCluster() == null) {
				throw new RuntimeException(
						"Host must have cluster name in order to use ClusterConfigBasedUrlClosure");
			}

			// find url
			String key = "turbine.instanceUrlSuffix." + host.getCluster();
			DynamicStringProperty urlClosureConfig = DynamicPropertyFactory.getInstance()
					.getStringProperty(key, null);
			String url = urlClosureConfig.get();
			if (url == null) {
				url = this.defaultUrlClosureConfig.get();
			}
			if (url == null) {
				throw new RuntimeException("Config property: "
						+ urlClosureConfig.getName() + " or "
						+ this.defaultUrlClosureConfig.getName() + " must be set");
			}

			// find port and scheme
			String port;
			String scheme;
			if (host.getAttributes().containsKey("securePort")) {
				port = host.getAttributes().get("securePort");
				scheme = "https";
			} else {
				port = host.getAttributes().get("port");
				scheme = "http";
			}

			if (host.getAttributes().containsKey("fusedHostPort")) {
				return String.format("%s://%s/%s", scheme, host.getAttributes().get("fusedHostPort"), url);
			}

			// determine if to insert port
			String insertPortKey = "turbine.instanceInsertPort." + host.getCluster();
			DynamicStringProperty insertPortProp = DynamicPropertyFactory.getInstance()
					.getStringProperty(insertPortKey, null);
			boolean insertPort;
			if (insertPortProp.get() == null) {
				insertPort = this.instanceInsertPort.get();
			}
			else {
				insertPort = Boolean.parseBoolean(insertPortProp.get());
			}

			// format url with port
			if (insertPort) {
				if (url.startsWith("/")) {
					url = url.substring(1);
				}
				if (port == null) {
					throw new RuntimeException(
							"Configured to use port, but port or securePort is not in host attributes");
				}

				return String.format("%s://%s:%s/%s", scheme, host.getHostname(), port, url);
			}

			//format url without port
			return scheme + "://" + host.getHostname() + url;
		}
	};

}
