/*
 * Copyright 2013-2016 the original author or authors.
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

package org.springframework.cloud.netflix.hystrix;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.catalina.core.ApplicationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.client.actuator.NamedFeature;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.Hystrix;
import com.netflix.hystrix.contrib.javanica.aop.aspectj.HystrixCommandAspect;
import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsPoller;
import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsPoller.MetricsAsJsonPollerListener;
import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsStreamServlet;

/**
 * @author Spencer Gibb
 * @author Christian Dupuis
 * @author Venil Noronha
 */
@Configuration
public class HystrixCircuitBreakerConfiguration {

	@Bean
	public HystrixCommandAspect hystrixCommandAspect() {
		return new HystrixCommandAspect();
	}

	@Bean
	public HystrixShutdownHook hystrixShutdownHook() {
		return new HystrixShutdownHook();
	}

	@Bean
	public HasFeatures hystrixFeature() {
		return HasFeatures.namedFeatures(new NamedFeature("Hystrix", HystrixCommandAspect.class));
	}

	@Configuration
	@ConditionalOnProperty(value = "hystrix.stream.endpoint.enabled", matchIfMissing = true)
	@ConditionalOnWebApplication
	@ConditionalOnClass({ Endpoint.class, HystrixMetricsStreamServlet.class })
	protected static class HystrixWebConfiguration {

		@Bean
		public HystrixStreamEndpoint hystrixStreamEndpoint() {
			return new HystrixStreamEndpoint();
		}

		@Bean
		public HasFeatures hystrixStreamFeature() {
			return HasFeatures.namedFeature("Hystrix Stream Servlet", HystrixStreamEndpoint.class);
		}
	}

	@Configuration
	@ConditionalOnProperty(value = "hystrix.metrics.enabled", matchIfMissing = true)
	@ConditionalOnClass({ HystrixMetricsPoller.class, GaugeService.class })
	@EnableConfigurationProperties(HystrixMetricsProperties.class)
	protected static class HystrixMetricsPollerConfiguration implements SmartLifecycle {

		private static Log logger = LogFactory
				.getLog(HystrixMetricsPollerConfiguration.class);

		@Autowired(required = false)
		private GaugeService gauges;

		@Autowired
		private HystrixMetricsProperties metricsProperties;

		private ObjectMapper mapper = new ObjectMapper();

		private HystrixMetricsPoller poller;

		private Set<String> reserved = new HashSet<String>(Arrays.asList("group", "name",
				"type", "currentTime"));

		@Override
		public void start() {
			if (this.gauges == null) {
				return;
			}
			MetricsAsJsonPollerListener listener = new MetricsAsJsonPollerListener() {
				@Override
				public void handleJsonMetric(String json) {
					try {
						@SuppressWarnings("unchecked")
						Map<String, Object> map = HystrixMetricsPollerConfiguration.this.mapper
								.readValue(json, Map.class);
						if (map != null && map.containsKey("type")) {
							addMetrics(map, "hystrix.");
						}
					}
					catch (IOException ex) {
						// ignore
					}
				}

			};
			this.poller = new HystrixMetricsPoller(listener,
					metricsProperties.getPollingIntervalMs());
			// start polling and it will write directly to the listener
			this.poller.start();
			logger.info("Starting poller");
		}

		private void addMetrics(Map<String, Object> map, String root) {
			StringBuilder prefixBuilder = new StringBuilder(root);
			if (map.containsKey("type")) {
				prefixBuilder.append((String) map.get("type"));
				if (map.containsKey("group")) {
					prefixBuilder.append(".").append(map.get("group"));
				}
				prefixBuilder.append(".").append(map.get("name"));
			}
			String prefix = prefixBuilder.toString();
			for (String key : map.keySet()) {
				Object value = map.get(key);
				if (!this.reserved.contains(key)) {
					if (value instanceof Number) {
						String name = prefix + "." + key;
						this.gauges.submit(name, ((Number) value).doubleValue());
					}
					else if (value instanceof Map) {
						@SuppressWarnings("unchecked")
						Map<String, Object> sub = (Map<String, Object>) value;
						addMetrics(sub, prefix);
					}
				}
			}
		}

		@Override
		public void stop() {
			if (this.poller != null) {
				this.poller.shutdown();
			}
		}

		@Override
		public boolean isRunning() {
			return this.poller != null ? this.poller.isRunning() : false;
		}

		@Override
		public int getPhase() {
			return Ordered.LOWEST_PRECEDENCE;
		}

		@Override
		public boolean isAutoStartup() {
			return true;
		}

		@Override
		public void stop(Runnable callback) {
			if (this.poller != null) {
				this.poller.shutdown();
			}
			callback.run();
		}

	}

	/**
	 * {@link DisposableBean} that makes sure that Hystrix internal state is cleared when
	 * {@link ApplicationContext} shuts down.
	 */
	private class HystrixShutdownHook implements DisposableBean {

		@Override
		public void destroy() throws Exception {
			// Just call Hystrix to reset thread pool etc.
			Hystrix.reset();
		}

	}

}
