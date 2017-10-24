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

import java.util.Collection;

import com.netflix.turbine.data.AggDataFromCluster;
import com.netflix.turbine.discovery.Instance;
import com.netflix.turbine.handler.PerformanceCriteria;
import com.netflix.turbine.handler.TurbineDataHandler;
import com.netflix.turbine.monitor.TurbineDataMonitor;
import com.netflix.turbine.monitor.cluster.AggregateClusterMonitor;
import com.netflix.turbine.monitor.cluster.ClusterMonitor;
import com.netflix.turbine.monitor.cluster.ClusterMonitorFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static com.netflix.turbine.monitor.cluster.AggregateClusterMonitor.AggregatorClusterMonitorConsole;

/**
 * @author Spencer Gibb
 */
public class SpringAggregatorFactory implements ClusterMonitorFactory<AggDataFromCluster> {

	private static final Log log = LogFactory.getLog(SpringAggregatorFactory.class);

	private final TurbineClustersProvider clustersProvider;

	public SpringAggregatorFactory(TurbineClustersProvider clustersProvider) {
		this.clustersProvider = clustersProvider;
	}

	/**
	 * @return {@link com.netflix.turbine.monitor.cluster.ClusterMonitor}<
	 * {@link com.netflix.turbine.data.AggDataFromCluster}>
	 */
	@Override
	public ClusterMonitor<AggDataFromCluster> getClusterMonitor(String name) {
		TurbineDataMonitor<AggDataFromCluster> clusterMonitor = AggregateClusterMonitor.AggregatorClusterMonitorConsole
				.findMonitor(name + "_agg");
		return (ClusterMonitor<AggDataFromCluster>) clusterMonitor;
	}

	public static TurbineDataMonitor<AggDataFromCluster> findOrRegisterAggregateMonitor(
			String clusterName) {
		TurbineDataMonitor<AggDataFromCluster> clusterMonitor = AggregatorClusterMonitorConsole
				.findMonitor(clusterName + "_agg");
		if (clusterMonitor == null) {
			log.info("Could not find monitors: "
					+ AggregatorClusterMonitorConsole.toString());
			clusterMonitor = new SpringClusterMonitor(clusterName + "_agg", clusterName);
			clusterMonitor = AggregatorClusterMonitorConsole
					.findOrRegisterMonitor(clusterMonitor);
		}
		return clusterMonitor;
	}

	@Override
	public void initClusterMonitors() {
		for (String clusterName : clustersProvider.getClusterNames()) {
			ClusterMonitor<AggDataFromCluster> clusterMonitor = (ClusterMonitor<AggDataFromCluster>) findOrRegisterAggregateMonitor(clusterName);
			clusterMonitor.registerListenertoClusterMonitor(this.StaticListener);
			try {
				clusterMonitor.startMonitor();
			}
			catch (Exception ex) {
				log.warn("Could not init cluster monitor for: " + clusterName);
				clusterMonitor.stopMonitor();
				clusterMonitor.getDispatcher().stopDispatcher();
			}
		}
	}

	/**
	 * shutdown all configured cluster monitors
	 */
	@Override
	public void shutdownClusterMonitors() {
		for (String clusterName : clustersProvider.getClusterNames()) {
			ClusterMonitor<AggDataFromCluster> clusterMonitor = (ClusterMonitor<AggDataFromCluster>) AggregateClusterMonitor
					.findOrRegisterAggregateMonitor(clusterName);
			clusterMonitor.stopMonitor();
			clusterMonitor.getDispatcher().stopDispatcher();
		}
	}

	private TurbineDataHandler<AggDataFromCluster> StaticListener = new TurbineDataHandler<AggDataFromCluster>() {

		@Override
		public String getName() {
			return "StaticListener_For_Aggregator";
		}

		@Override
		public void handleData(Collection<AggDataFromCluster> stats) {
		}

		@Override
		public void handleHostLost(Instance host) {
		}

		@Override
		public PerformanceCriteria getCriteria() {
			return SpringAggregatorFactory.this.NonCriticalCriteria;
		}

	};

	private PerformanceCriteria NonCriticalCriteria = new PerformanceCriteria() {

		@Override
		public boolean isCritical() {
			return false;
		}

		@Override
		public int getMaxQueueSize() {
			return 0;
		}

		@Override
		public int numThreads() {
			return 0;
		}

	};
}
