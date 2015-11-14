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

import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.monitor.Monitor;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.TagList;

/**
 * @author Spencer Gibb
 */
public class HierarchicalServoMetricNaming implements ServoMetricNaming {
	private static final String JMX_DOMAIN_KEY = "JmxDomain";
	public static final String SERVO = "servo.";

	@Override
	public String asHierarchicalName(Monitor<?> monitor) {
		MonitorConfig config = monitor.getConfig();
		TagList tags = config.getTags();

		Tag domainTag = tags.getTag(JMX_DOMAIN_KEY);
		String name;
		if (domainTag != null) { // jmx metric
			name = handleJmxMetric(config, tags);
		} else {
			name = handleMetric(config, tags);
		}
		return name.toLowerCase();
	}

	private String handleMetric(MonitorConfig config, TagList tags) {
		String type = cleanValue(tags.getTag(DataSourceType.KEY), false);
		String instanceName = cleanValue(tags.getTag("instance"), false);
		String name = cleanupIllegalCharacters(config.getName(), true);
		String statistic = cleanValue(tags.getTag("statistic"), false);

		StringBuilder nameBuilder = new StringBuilder();
		if (type != null) {
			nameBuilder.append(type).append(".");
		}
		nameBuilder.append(SERVO);
		if (instanceName != null) {
			nameBuilder.append(instanceName).append(".");
		}
		if (name != null) {
			nameBuilder.append(name).append(".");
		}
		if (statistic != null) {
			nameBuilder.append(statistic).append(".");
		}
		// remove trailing "."
		nameBuilder.deleteCharAt(nameBuilder.lastIndexOf("."));
		return nameBuilder.toString();
	}

	private String handleJmxMetric(MonitorConfig config, TagList tags) {
		String domain = cleanValue(tags.getTag(JMX_DOMAIN_KEY), true);
		String type = cleanValue(tags.getTag("Jmx.type"), false);
		String instanceName = cleanValue(tags.getTag("Jmx.instance"), false);
		String name = cleanValue(tags.getTag("Jmx.name"), true);
		String fieldName = cleanupIllegalCharacters(config.getName(), false);

		StringBuilder nameBuilder = new StringBuilder();
		nameBuilder.append(domain).append(".");
		if (type != null) {
			nameBuilder.append(type).append(".");
		}
		nameBuilder.append(SERVO);
		if (instanceName != null) {
			nameBuilder.append(instanceName).append(".");
		}
		if (name != null) {
			nameBuilder.append(name).append(".");
		}
		if (fieldName != null) {
			nameBuilder.append(fieldName).append(".");
		}
		// remove trailing "."
		nameBuilder.deleteCharAt(nameBuilder.lastIndexOf("."));
		return nameBuilder.toString();
	}

	private String cleanValue(Tag tag, boolean allowPeriodsInName) {
		if (tag == null) {
			return null;
		}

		return cleanupIllegalCharacters(tag.getValue(), allowPeriodsInName);
	}

	private String cleanupIllegalCharacters(String s, boolean allowPeriodsInName) {
		if (!allowPeriodsInName) {
			s = s.replace(".", "_");
		}
		return s.replace(" ", "_");
	}
}