/*
 * Copyright 2018-2019 the original author or authors.
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
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.netflix.turbine.data.AggDataFromCluster;
import com.netflix.turbine.data.DataFromSingleInstance;
import com.netflix.turbine.monitor.MonitorConsole;
import com.netflix.turbine.monitor.TurbineDataMonitor;
import com.netflix.turbine.monitor.cluster.ClusterMonitor;

import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import static com.netflix.turbine.monitor.cluster.AggregateClusterMonitor.AggregatorClusterMonitorConsole;

/**
 * Service providing information on Turbine clusters.
 *
 * @author Anastasiia Smirnova
 */
public class TurbineInformationService {

	public Collection<ClusterInformation> getClusterInformations(
			HttpServletRequest request) {
		String hostName = getHostName(request);
		return getClusterInformations(hostName);
	}

	private Collection<ClusterInformation> getClusterInformations(String hostName) {
		Collection<TurbineDataMonitor<AggDataFromCluster>> monitors = AggregatorClusterMonitorConsole
				.getAllMonitors();
		List<ClusterInformation> clusterInformations = new ArrayList<>();
		for (TurbineDataMonitor<AggDataFromCluster> monitor : monitors) {
			ClusterMonitor clusterMonitor = (ClusterMonitor) monitor;
			MonitorConsole<DataFromSingleInstance> instanceConsole = clusterMonitor
					.getInstanceMonitors();
			for (TurbineDataMonitor<DataFromSingleInstance> single : instanceConsole
					.getAllMonitors()) {
				String cluster = single.getStatsInstance().getCluster();
				ClusterInformation info = new ClusterInformation(cluster,
						getLink(hostName, cluster));
				clusterInformations.add(info);
			}
		}
		return clusterInformations;
	}

	private String getLink(String hostName, String cluster) {
		return hostName + "/turbine.stream?cluster=" + cluster;
	}

	private String getHostName(HttpServletRequest request) {
		UriComponents components = UriComponentsBuilder
				.fromHttpRequest(new ServletServerHttpRequest(request)).build();
		String host = components.getHost();
		int port = components.getPort();
		String scheme = components.getScheme();
		if (port > -1) {
			return String.format("%s://%s:%d", scheme, host, port);
		}
		return String.format("%s://%s", scheme, host);
	}

}
