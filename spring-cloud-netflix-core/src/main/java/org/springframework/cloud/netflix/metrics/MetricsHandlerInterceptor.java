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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.metrics.servo.ServoMonitorCache;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.netflix.servo.MonitorRegistry;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.tag.SmallTagMap;
import com.netflix.servo.tag.Tags;

import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

/**
 * Intercepts incoming HTTP requests and records metrics about execution time and results.
 *
 * @author Jon Schneider
 */
public class MetricsHandlerInterceptor extends HandlerInterceptorAdapter {
	@Value("${netflix.metrics.rest.metricName:rest}")
	String metricName;

	@Value("${netflix.metrics.rest.callerHeader:#{null}}")
	String callerHeader;

	@Autowired
	MonitorRegistry registry;

	@Autowired
	ServoMonitorCache servoMonitorCache;

	@Autowired
	Collection<MetricsTagProvider> tagProviders;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
			Object handler) throws Exception {
		RequestContextHolder.getRequestAttributes().setAttribute("requestStartTime",
				System.nanoTime(), SCOPE_REQUEST);
		return super.preHandle(request, response, handler);
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
			Object handler, Exception ex) throws Exception {
		RequestContextHolder.getRequestAttributes().setAttribute("exception", ex,
				SCOPE_REQUEST);
		Long startTime = (Long) RequestContextHolder.getRequestAttributes().getAttribute(
				"requestStartTime", SCOPE_REQUEST);
		if (startTime != null)
			recordMetric(request, response, handler, startTime);
		super.afterCompletion(request, response, handler, ex);
	}

	protected void recordMetric(HttpServletRequest request, HttpServletResponse response,
			Object handler, Long startTime) {
		String caller = null;
		if (callerHeader != null) {
			caller = request.getHeader(callerHeader);
		}

		SmallTagMap.Builder builder = SmallTagMap.builder();
		for (MetricsTagProvider tagProvider : tagProviders) {
			Map<String, String> tags = tagProvider.httpRequestTags(request, response,
					handler, caller);
			for (Map.Entry<String, String> tag : tags.entrySet()) {
				builder.add(Tags.newTag(tag.getKey(), tag.getValue()));
			}
		}

		MonitorConfig.Builder monitorConfigBuilder = MonitorConfig.builder(metricName);
		monitorConfigBuilder.withTags(builder);

		servoMonitorCache.getTimer(monitorConfigBuilder.build()).record(
				System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
	}
}
