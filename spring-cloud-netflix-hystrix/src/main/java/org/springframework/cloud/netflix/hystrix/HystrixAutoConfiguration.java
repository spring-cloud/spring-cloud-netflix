/*
 * Copyright 2013-2018 the original author or authors.
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
 *
 */

package org.springframework.cloud.netflix.hystrix;

import com.netflix.hystrix.Hystrix;
import com.netflix.hystrix.contrib.javanica.aop.aspectj.HystrixCommandAspect;
import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsStreamServlet;
import com.netflix.hystrix.metric.consumer.HystrixDashboardStream;
import com.netflix.hystrix.serial.SerialHystrixDashboardData;
import io.micrometer.core.instrument.binder.hystrix.HystrixMetricsBinder;
import org.reactivestreams.Publisher;
import rx.Observable;
import rx.RxReactiveStreams;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.autoconfigure.health.HealthIndicatorAutoConfiguration;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.DispatcherHandler;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.REACTIVE;
import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET;

/**
 * Auto configuration for Hystrix.
 *
 * @author Christian Dupuis
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass({ Hystrix.class, HealthIndicator.class, HealthIndicatorAutoConfiguration.class })
@AutoConfigureAfter({ HealthIndicatorAutoConfiguration.class })
public class HystrixAutoConfiguration {

	@Bean
	@ConditionalOnEnabledHealthIndicator("hystrix")
	public HystrixHealthIndicator hystrixHealthIndicator() {
		return new HystrixHealthIndicator();
	}

	@Configuration
	@ConditionalOnProperty(value = "management.metrics.binders.hystrix.enabled", matchIfMissing = true)
	@ConditionalOnClass({ HystrixMetricsBinder.class })
	protected static class HystrixMetricsConfiguration {
		@Bean
		public HystrixMetricsBinder hystrixMetricsBinder() {
			return new HystrixMetricsBinder();
		}
	}

	/**
	 * See original {@link org.springframework.boot.actuate.autoconfigure.jolokia.JolokiaEndpointAutoConfiguration}
	 */
	@Configuration
	@ConditionalOnWebApplication(type = SERVLET)
	@ConditionalOnBean(HystrixCommandAspect.class) // only install the stream if enabled
	@ConditionalOnClass({ HystrixMetricsStreamServlet.class })
	@EnableConfigurationProperties(HystrixProperties.class)
	protected static class HystrixServletAutoConfiguration {

		@Bean
		@ConditionalOnEnabledEndpoint
		public HystrixStreamEndpoint hystrixStreamEndpoint(HystrixProperties properties) {
			return new HystrixStreamEndpoint(properties.getConfig());
		}

		@Bean
		public HasFeatures hystrixStreamFeature() {
			return HasFeatures.namedFeature("Hystrix Stream Servlet", HystrixMetricsStreamServlet.class);
		}
	}

	@Configuration
	@ConditionalOnWebApplication(type = REACTIVE)
	@ConditionalOnBean(HystrixCommandAspect.class) // only install the stream if enabled
	@ConditionalOnClass({ DispatcherHandler.class })
	@EnableConfigurationProperties(HystrixProperties.class)
	protected static class HystrixWebfluxManagementContextConfiguration {

		@Bean
		@ConditionalOnEnabledEndpoint
		public HystrixWebfluxEndpoint hystrixWebfluxController() {
			Observable<String> serializedDashboardData = HystrixDashboardStream.getInstance().observe()
					.concatMap(dashboardData -> Observable.from(SerialHystrixDashboardData.toMultipleJsonStrings(dashboardData)));
			Publisher<String> publisher = RxReactiveStreams.toPublisher(serializedDashboardData);
			return new HystrixWebfluxEndpoint(publisher);
		}

		@Bean
		public HasFeatures hystrixStreamFeature() {
			return HasFeatures.namedFeature("Hystrix Stream Webflux", HystrixWebfluxEndpoint.class);
		}
	}
}
