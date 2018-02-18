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

package org.springframework.cloud.netflix.turbine.stream;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.MapPropertySource;

public class TurbinePortApplicationListener implements
		ApplicationListener<ApplicationEnvironmentPreparedEvent> {

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		Integer serverPort = event.getEnvironment().getProperty("server.port",
				Integer.class);
		Integer managementPort = event.getEnvironment().getProperty("management.port",
				Integer.class);
		Integer turbinePort = event.getEnvironment().getProperty("turbine.stream.port",
				Integer.class);
		if (serverPort == null && managementPort == null) {
			return;
		}
		if (serverPort != Integer.valueOf(-1)) {
			Map<String, Object> ports = new HashMap<>();
			if (turbinePort == null) {
				// The actual server.port used by the application forced to be -1 (no user
				// endpoints) because no value was provided for turbine
				ports.put("server.port", -1);
				if (serverPort != null) {
					// Turbine port defaults to server port value supplied by user
					ports.put("turbine.stream.port", serverPort);
				}
			}
			else if (managementPort != null && managementPort != -1 && serverPort == null) {
				// User wants 2 ports, but hasn't specified server.port explicitly
				ports.put("server.port", managementPort);
			}
			event.getEnvironment().getPropertySources()
					.addFirst(new MapPropertySource("ports", ports));
		}
	}

}
