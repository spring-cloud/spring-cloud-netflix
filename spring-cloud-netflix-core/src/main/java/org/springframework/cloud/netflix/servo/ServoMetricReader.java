/*
 * Copyright 2013-2014 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.reader.MetricReader;

/**
 * @author Dave Syer
 *
 */
public class ServoMetricReader implements MetricReader {

	private final MBeanServer server;

	public ServoMetricReader(MBeanServer server) {
		this.server = server;
	}

	@Override
	public Metric<?> findOne(String metricName) {
		return getServoMetrics().get(metricName);
	}

	@Override
	public Iterable<Metric<?>> findAll() {
		return getServoMetrics().values();
	}

	@Override
	public long count() {
		return getServoMetrics().size();
	}
	
	private Map<String,Metric<?>> getServoMetrics() {
		Map<String, Metric<?>> metrics = getServoMetrics("COUNTER");
		metrics.putAll(getServoMetrics("GAUGE"));
		return metrics;
	}
	
	private Map<String,Metric<?>> getServoMetrics(String type) {
		Map<String,Metric<?>> metrics = new HashMap<String, Metric<?>>();
		try {
			ObjectName name = new ObjectName("com.netflix.servo:type="+type+",*");
			Set<ObjectInstance> beans = server.queryMBeans(name, null);
			for (ObjectInstance bean : beans) {
				// example: com.netflix.servo:name=DiscoveryClient_Failed,class=DiscoveryClient,type=COUNTER
				String key = type.toLowerCase() + ".servo."
						+ bean.getObjectName().getKeyProperty("name");
				Object attribute = server.getAttribute(bean.getObjectName(), "value");
				if (attribute instanceof Number) {
					Number value = (Number) attribute;
					metrics.put(key, new Metric<Number>(key, value));
				}
			}
		}
		catch (Exception e) {
			// Really?
		}
		return metrics;
	}

}
