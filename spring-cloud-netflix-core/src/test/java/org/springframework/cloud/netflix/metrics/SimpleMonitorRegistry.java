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

package org.springframework.cloud.netflix.metrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.netflix.servo.MonitorRegistry;
import com.netflix.servo.monitor.Monitor;

/**
 * @author Jon Schneider
 */
public class SimpleMonitorRegistry implements MonitorRegistry {
	List<Monitor<?>> monitors = new ArrayList<>();

	@Override
	public Collection<Monitor<?>> getRegisteredMonitors() {
		return monitors;
	}

	@Override
	public void register(Monitor<?> monitor) {
		monitors.add(monitor);
	}

	@Override
	public void unregister(Monitor<?> monitor) {
		monitors.remove(monitor);
	}

	@Override
	public boolean isRegistered(Monitor<?> monitor) {
		for (Monitor<?> m : monitors) {
			if (m.equals(monitor))
				return true;
		}
		return false;
	}
}