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

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.StringUtils;

import com.netflix.servo.monitor.Monitor;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.tag.Tag;

/**
 * @author Jon Schneider
 */
public class DimensionalServoMetricNaming implements ServoMetricNaming {
	@Override
	public String asHierarchicalName(Monitor<?> monitor) {
		MonitorConfig config = monitor.getConfig();
		List<String> tags = new ArrayList<>(config.getTags().size());
		for (Tag t : config.getTags()) {
			tags.add(t.getKey() + "=" + t.getValue());
		}
		return config.getName() + "(" + StringUtils.collectionToCommaDelimitedString(tags) + ")";
	}
}
