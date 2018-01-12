/*
 * Copyright 2014-2018 the original author or authors.
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

import org.reactivestreams.Publisher;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.DispatcherHandler;

import com.netflix.hystrix.contrib.javanica.aop.aspectj.HystrixCommandAspect;
import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsStreamServlet;
import com.netflix.hystrix.metric.consumer.HystrixDashboardStream;
import com.netflix.hystrix.serial.SerialHystrixDashboardData;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.REACTIVE;

import rx.Observable;
import rx.RxReactiveStreams;

/**
 * See original {@link org.springframework.boot.actuate.autoconfigure.jolokia.JolokiaManagementContextConfiguration}
 */
@ManagementContextConfiguration
@ConditionalOnProperty(value = "management.hystrix.enabled", matchIfMissing = true)
@ConditionalOnWebApplication(type = REACTIVE)
@ConditionalOnBean(HystrixCommandAspect.class) // only install the stream if enabled
@ConditionalOnClass({ Health.class, DispatcherHandler.class })
@EnableConfigurationProperties(HystrixProperties.class)
class HystrixWebfluxManagementContextConfiguration {

	@Bean
	public HystrixWebfluxController hystrixWebfluxController() {
		Observable<String> serializedDashboardData = HystrixDashboardStream.getInstance().observe()
				.concatMap(dashboardData -> Observable.from(SerialHystrixDashboardData.toMultipleJsonStrings(dashboardData)));
		Publisher<String> publisher = RxReactiveStreams.toPublisher(serializedDashboardData);
		return new HystrixWebfluxController(publisher);
	}

	@Bean
	public HasFeatures hystrixStreamFeature() {
		return HasFeatures.namedFeature("Hystrix Stream Webflux", HystrixMetricsStreamServlet.class);
	}
}
