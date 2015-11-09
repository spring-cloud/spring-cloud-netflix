/*
 * Copyright 2013-2015 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.cloud.netflix.metrics.servo;

import java.util.HashMap;
import java.util.Map;

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.MonitorRegistry;
import com.netflix.servo.monitor.BasicTimer;
import com.netflix.servo.monitor.Monitor;
import com.netflix.servo.monitor.MonitorConfig;

/**
 * Servo does not provide a mechanism to retrieve an existing monitor by name + tags.
 *
 * @author Jon Schneider
 */
public class ServoMonitorCache {
	private static final Map<MonitorConfig, BasicTimer> timerCache = new HashMap<>();

	/**
	 * @param config contains the name and tags that uniquely identify a timer
	 * @return an already registered timer if it exists, otherwise create/register one and
	 * return it.
	 */
	public synchronized static BasicTimer getTimer(MonitorConfig config) {
		BasicTimer t = timerCache.get(config);
		if (t != null)
			return t;

		t = new BasicTimer(config);
		timerCache.put(config, t);
		DefaultMonitorRegistry.getInstance().register(t);
		return t;
	}

	/**
	 * Useful for tests to clear the monitor registry between runs
	 */
	public static void unregisterAll() {
		MonitorRegistry registry = DefaultMonitorRegistry.getInstance();
		for (Monitor<?> monitor : registry.getRegisteredMonitors()) {
			registry.unregister(monitor);
		}
		timerCache.clear();
	}
}
