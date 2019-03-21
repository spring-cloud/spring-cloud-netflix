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

import com.netflix.turbine.discovery.InstanceDiscovery;
import com.netflix.turbine.init.TurbineInit;
import com.netflix.turbine.monitor.cluster.ClusterMonitorFactory;
import com.netflix.turbine.plugins.PluginsFactory;

import org.springframework.context.SmartLifecycle;
import org.springframework.core.Ordered;

/**
 * @author Spencer Gibb
 */
public class TurbineLifecycle implements SmartLifecycle, Ordered {

	private final InstanceDiscovery instanceDiscovery;

	private final ClusterMonitorFactory<?> factory;

	private volatile boolean running;

	public TurbineLifecycle(InstanceDiscovery instanceDiscovery,
			ClusterMonitorFactory<?> factory) {
		this.instanceDiscovery = instanceDiscovery;
		this.factory = factory;
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public void stop(Runnable callback) {
		callback.run();
	}

	@Override
	public void start() {
		PluginsFactory.setClusterMonitorFactory(factory);
		PluginsFactory.setInstanceDiscovery(instanceDiscovery);
		TurbineInit.init();
	}

	@Override
	public void stop() {
		this.running = false;
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public int getPhase() {
		return 0;
	}

	@Override
	public int getOrder() {
		return -1;
	}

}
