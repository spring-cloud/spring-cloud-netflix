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

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.cloud.netflix.metrics.servo.ServoMonitorCache;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.tag.SmallTagMap;
import com.netflix.servo.tag.Tags;

/**
 * Intercepts RestTemplate requests and records metrics about execution time and results.
 *
 * @author Jon Schneider
 */
public class MetricsClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {
	/**
	 * The interceptor writes to a Servo MonitorRegistry, which we get away with for now
	 * because our Spectator implementation is underpinned by a ServoRegistry. When Spring
	 * Boot (Actuator) provides a more general purpose abstraction for dimensional metrics
	 * systems, this can be moved there and rewritten against that abstraction.
	 */
	private final ServoMonitorCache servoMonitorCache;

	private final Collection<MetricsTagProvider> tagProviders;

	private final String metricName;

	public MetricsClientHttpRequestInterceptor(
			Collection<MetricsTagProvider> tagProviders,
			ServoMonitorCache servoMonitorCache, String metricName) {
		this.tagProviders = tagProviders;
		this.servoMonitorCache = servoMonitorCache;
		this.metricName = metricName;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body,
			ClientHttpRequestExecution execution) throws IOException {
		long startTime = System.nanoTime();

		ClientHttpResponse response = null;
		try {
			response = execution.execute(request, body);
			return response;
		}
		finally {
			SmallTagMap.Builder builder = SmallTagMap.builder();
			for (MetricsTagProvider tagProvider : tagProviders) {
				for (Map.Entry<String, String> tag : tagProvider
						.clientHttpRequestTags(request, response).entrySet()) {
					builder.add(Tags.newTag(tag.getKey(), tag.getValue()));
				}
			}

			MonitorConfig.Builder monitorConfigBuilder = MonitorConfig
					.builder(metricName);
			monitorConfigBuilder.withTags(builder);

			servoMonitorCache.getTimer(monitorConfigBuilder.build())
					.record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
		}
	}
}
