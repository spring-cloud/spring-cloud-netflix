package org.springframework.cloud.netflix.hystrix;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.contrib.javanica.aop.aspectj.HystrixCommandAspect;
import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsPoller;
import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsPoller.MetricsAsJsonPollerListener;

/**
 * @author Spencer Gibb
 */
@Configuration
public class HystrixConfiguration implements ImportAware {

	private AnnotationAttributes enableHystrix;

	@Bean
	HystrixCommandAspect hystrixCommandAspect() {
		return new HystrixCommandAspect();
	}

	@Bean
	// TODO: add enable/disable
	// TODO: make it @ConditionalOnWebApp (need a nested class)
	public HystrixStreamEndpoint hystrixStreamEndpoint() {
		return new HystrixStreamEndpoint();
	}

	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		this.enableHystrix = AnnotationAttributes.fromMap(importMetadata
				.getAnnotationAttributes(EnableHystrix.class.getName(), false));
		Assert.notNull(
				this.enableHystrix,
				"@EnableHystrix is not present on importing class "
						+ importMetadata.getClassName());
	}

	@Autowired(required = false)
	void setConfigurers(Collection<HystrixConfigurer> configurers) {
		if (CollectionUtils.isEmpty(configurers)) {
			return;
		}
		if (configurers.size() > 1) {
			throw new IllegalStateException(
					"Only one HystrixConfigurer may exist");
		}
		// TODO: create CircuitBreakerConfigurer API
		// CircuitBreakerConfigurer configurer = configurers.iterator().next();
		// this.txManager = configurer.annotationDrivenTransactionManager();
	}

	@Configuration
	@ConditionalOnClass(GaugeService.class)
	protected static class HystrixMetricsPollerConfiguration implements SmartLifecycle {

		private static Log logger = LogFactory
				.getLog(HystrixMetricsPollerConfiguration.class);

		@Autowired
		private GaugeService gauges;

		private ObjectMapper mapper = new ObjectMapper();

		private HystrixMetricsPoller poller;

		private Set<String> reserved = new HashSet<String>(Arrays.asList("group", "name",
				"type", "currentTime"));

		@Override
		public void start() {
			MetricsAsJsonPollerListener listener = new MetricsAsJsonPollerListener() {
				@Override
				public void handleJsonMetric(String json) {
					try {
						@SuppressWarnings("unchecked")
						Map<String, Object> map = mapper.readValue(json, Map.class);
						if (map != null && map.containsKey("type")) {
							addMetrics(map, "hystrix.");
						}
					}
					catch (IOException e) {
						// ignore
					}
				}

			};
			poller = new HystrixMetricsPoller(listener, 2000);
			// start polling and it will write directly to the listener
			poller.start();
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
				if (!reserved.contains(key)) {
					if (value instanceof Number) {
						String name = prefix + "." + key;
						gauges.submit(name, ((Number) value).doubleValue());
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
			if (poller != null) {
				poller.shutdown();
			}
		}

		@Override
		public boolean isRunning() {
			return poller!=null ?  poller.isRunning() : false;
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
			if (poller != null) {
				poller.shutdown();
			}
			callback.run();
		}

	}
}
