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

package org.springframework.cloud.netflix.servo;

import com.netflix.servo.Metric;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.TagList;

/**
 * @author Spencer Gibb
 */
public class DefaultServoMetricNaming implements ServoMetricNaming {
	private static final String JMX_DOMAIN_KEY = "JmxDomain";
	public static final String SERVO = "servo";

	protected String servoPrefix;
	
	/**
	 * Naming strategy using the default servo prefix
	 */
	public DefaultServoMetricNaming() {
		this(SERVO);
	}
	
	/**
	 * Construct a naming strategy with the supplied servo prefix.
	 * 
	 * @param servoPrefix the prefix to append to metric name (after the metric type)
	 */
	public DefaultServoMetricNaming(String servoPrefix) {
		this.servoPrefix = servoPrefix;
	}
	
	/**
	 * Get the metric name for the given servo metric.
	 * @param metric servo metric whose name must be computed
	 * @return the metric name
	 */
	@Override
	public String getName(Metric metric) {
		MonitorConfig config = metric.getConfig();
		TagList tags = config.getTags();

		Tag domainTag = tags.getTag(JMX_DOMAIN_KEY);
		String name;
		if (domainTag != null) { // jmx metric
			name = handleJmxMetric(config, tags);
		}
		else {
			name = handleMetric(config, tags);
		}
		return name.toLowerCase();
	}

	
	/**
	 * Handle a non-JMX servo metric
	 */
	protected String handleMetric(MonitorConfig config, TagList tags) {
		return new NameBuilder()
					.append(cleanValue(tags.getTag(DataSourceType.KEY), false))
					.append(servoPrefix)
					.append(cleanValue(tags.getTag("instance"), false))
					.append(cleanValue(config.getName(), true))
					.append(cleanValue(tags.getTag("statistic"), false))
					.build();
	}

	/**
	 * Handle a JMX servo metric
	 */
	protected String handleJmxMetric(MonitorConfig config, TagList tags) {	
		return new NameBuilder()
					.append(cleanValue(tags.getTag(JMX_DOMAIN_KEY), true))
					.append(servoPrefix)
					.append(cleanValue(tags.getTag("Jmx.instance"), false))
					.append(cleanValue(tags.getTag("Jmx.name"), false))
					.append(cleanValue(config.getName(), false))
					.build();
	}

	/**
	 * Get and clean the value of the given tag
	 */
	protected String cleanValue(Tag tag, boolean allowPeriodsInName) {
		if (tag == null) {
			return null;
		}

		return cleanValue(tag.getValue(), allowPeriodsInName);
	}

	/**
	 * Clean the input string by replacing invalid characters
	 */
	protected String cleanValue(String s, boolean allowPeriodsInName) {
		if( s == null ) {
			return null;
		}
		if (!allowPeriodsInName) {
			s = s.replace(".", "_");
		}
		return s.replace(" ", "_");
	}
	
	protected static class NameBuilder {
		private StringBuilder builder = new StringBuilder();
		
		public NameBuilder append(String value) {
			if( value != null ) {
				if( builder.length() > 0 ) {
					append(".");
				}
				append(value);
			}
			return this;
		}
		
		public String build() {
			return builder.toString();
		}
	}
}
