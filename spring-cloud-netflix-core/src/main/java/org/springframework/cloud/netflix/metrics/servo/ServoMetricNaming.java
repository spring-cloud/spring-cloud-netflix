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

package org.springframework.cloud.netflix.metrics.servo;

import com.netflix.servo.monitor.Monitor;

/**
 * @author Spencer Gibb
 */
public interface ServoMetricNaming {
	/**
	 * @param monitor a monitor representing a single statistic (not a CompositeMonitor)
	 * @return a hierarchical name representing a single statistic for a servo monitor;
	 * note that this method will be called once for each statistic on a composite servo
	 * Monitor like a Timer.
	 */
	String asHierarchicalName(Monitor<?> monitor);
}