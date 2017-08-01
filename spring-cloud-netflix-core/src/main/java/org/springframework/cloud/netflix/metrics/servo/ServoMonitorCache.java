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

import com.netflix.servo.MonitorRegistry;
import com.netflix.servo.monitor.BasicTimer;
import com.netflix.servo.monitor.MonitorConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Servo does not provide a mechanism to retrieve an existing monitor by name + tags.
 *
 * @author Jon Schneider
 */
public class ServoMonitorCache {

	private static final Log log = LogFactory.getLog(ServoMonitorCache.class);

	private final Map<MonitorConfig, BasicTimer> timerCache = new HashMap<>();
	private final MonitorRegistry monitorRegistry;
	private final ServoMetricsConfigBean config;

	public ServoMonitorCache(MonitorRegistry monitorRegistry, ServoMetricsConfigBean config) {
		this.monitorRegistry = monitorRegistry;
		this.config = config;
	}

	/**
	 * @param config contains the name and tags that uniquely identify a timer
	 * @return an already registered timer if it exists, otherwise create/register one and
	 * return it.
	 */
	public synchronized BasicTimer getTimer(MonitorConfig config) {
		BasicTimer t = this.timerCache.get(config);
		if (t != null)
			return t;

		t = new BasicTimer(config);
		this.timerCache.put(config, t);

		if (this.timerCache.size() > this.config.getCacheWarningThreshold()) {
			log.warn("timerCache is above the warning threshold of " + this.config.getCacheWarningThreshold() + " with size " + this.timerCache.size() + ".");
		}

		this.monitorRegistry.register(t);
		return t;
	}
}
