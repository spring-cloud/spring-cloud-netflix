/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.eureka;

import java.util.List;

import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.AbstractReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.OrderedHealthAggregator;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.actuate.health.SimpleStatusAggregator;
import org.springframework.cloud.client.discovery.health.DiscoveryClientHealthIndicator;
import org.springframework.cloud.client.discovery.health.DiscoveryCompositeHealthIndicator;
import org.springframework.cloud.client.discovery.health.DiscoveryHealthIndicator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link EurekaHealthCheckHandler} with different health indicator registered.
 *
 * @author Jakub Narloch
 * @author Nowrin Anwar Joyita
 */
public class EurekaHealthCheckHandlerTests {

	private EurekaHealthCheckHandler healthCheckHandler;

	private EurekaHealthCheckHandler healthCheckHandlerWithStatusAggregator;

	@Before
	public void setUp() {
		healthCheckHandler = new EurekaHealthCheckHandler(new OrderedHealthAggregator());
		healthCheckHandlerWithStatusAggregator = new EurekaHealthCheckHandler(
				new SimpleStatusAggregator());
	}

	@Test
	public void testNoHealthCheckRegistered() {
		InstanceStatus status = healthCheckHandler.getStatus(InstanceStatus.UNKNOWN);
		assertThat(status).isEqualTo(InstanceStatus.UNKNOWN);
	}

	@Test
	public void testAllUp() throws Exception {
		initialize(UpHealthConfiguration.class);

		InstanceStatus status = healthCheckHandler.getStatus(InstanceStatus.UNKNOWN);
		assertThat(status).isEqualTo(InstanceStatus.UP);
	}

	@Test
	public void testDownWithBlockingIndicators() throws Exception {
		initialize(UpHealthConfiguration.class, DownHealthConfiguration.class);

		InstanceStatus status = healthCheckHandler.getStatus(InstanceStatus.UNKNOWN);
		assertThat(status).isEqualTo(InstanceStatus.DOWN);
	}

	@Test
	public void testDownWithReactiveIndicators() throws Exception {
		initialize(ReactiveUpHealthConfiguration.class,
				ReactiveDownHealthConfiguration.class);

		InstanceStatus status = healthCheckHandlerWithStatusAggregator
				.getStatus(InstanceStatus.UNKNOWN);
		assertThat(status).isEqualTo(InstanceStatus.DOWN);
	}

	@Test
	public void testDownWhenBlockingIndicatorUpAndReactiveDown() throws Exception {
		initialize(UpHealthConfiguration.class, ReactiveDownHealthConfiguration.class);

		InstanceStatus status = healthCheckHandlerWithStatusAggregator
				.getStatus(InstanceStatus.UNKNOWN);
		assertThat(status).isEqualTo(InstanceStatus.DOWN);
	}

	@Test
	public void testDownWhenBlockingIndicatorDownAndReactiveUp() throws Exception {
		initialize(DownHealthConfiguration.class, ReactiveUpHealthConfiguration.class);

		InstanceStatus status = healthCheckHandlerWithStatusAggregator
				.getStatus(InstanceStatus.UNKNOWN);
		assertThat(status).isEqualTo(InstanceStatus.DOWN);
	}

	@Test
	public void testUnknown() throws Exception {
		initialize(FatalHealthConfiguration.class);

		InstanceStatus status = healthCheckHandler.getStatus(InstanceStatus.UNKNOWN);
		assertThat(status).isEqualTo(InstanceStatus.UNKNOWN);
	}

	@Test
	public void testEurekaIgnored() throws Exception {
		initialize(EurekaDownHealthConfiguration.class);

		InstanceStatus status = healthCheckHandler.getStatus(InstanceStatus.UP);
		assertThat(status).isEqualTo(InstanceStatus.UP);
	}

	private void initialize(Class<?>... configurations) throws Exception {
		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				configurations);
		healthCheckHandler.setApplicationContext(applicationContext);
		healthCheckHandler.afterPropertiesSet();
		healthCheckHandlerWithStatusAggregator.setApplicationContext(applicationContext);
		healthCheckHandlerWithStatusAggregator.afterPropertiesSet();
	}

	public static class UpHealthConfiguration {

		@Bean
		public HealthIndicator healthIndicator() {
			return new AbstractHealthIndicator() {
				@Override
				protected void doHealthCheck(Health.Builder builder) throws Exception {
					builder.up();
				}
			};
		}

	}

	public static class DownHealthConfiguration {

		@Bean
		public HealthIndicator healthIndicator() {
			return new AbstractHealthIndicator() {
				@Override
				protected void doHealthCheck(Health.Builder builder) throws Exception {
					builder.down();
				}
			};
		}

	}

	public static class FatalHealthConfiguration {

		@Bean
		public HealthIndicator healthIndicator() {
			return new AbstractHealthIndicator() {
				@Override
				protected void doHealthCheck(Health.Builder builder) throws Exception {
					builder.status("fatal");
				}
			};
		}

	}

	public static class ReactiveUpHealthConfiguration {

		@Bean
		public ReactiveHealthIndicator reactiveHealthIndicator() {
			return new AbstractReactiveHealthIndicator() {
				@Override
				protected Mono<Health> doHealthCheck(Health.Builder builder) {
					return Mono.just(builder.up().build());
				}
			};
		}

	}

	public static class ReactiveDownHealthConfiguration {

		@Bean
		public ReactiveHealthIndicator reactiveHealthIndicator() {
			return new AbstractReactiveHealthIndicator() {
				@Override
				protected Mono<Health> doHealthCheck(Health.Builder builder) {
					return Mono.just(builder.down().build());
				}
			};
		}

	}

	public static class EurekaDownHealthConfiguration {

		@Bean
		public DiscoveryHealthIndicator discoveryHealthIndicator() {
			return new DiscoveryClientHealthIndicator(null, null) {
				@Override
				public Health health() {
					return Health.up().build();
				}
			};
		}

		@Bean
		public DiscoveryHealthIndicator eurekaHealthIndicator() {
			return new EurekaHealthIndicator(null, null, null) {
				@Override
				public Health health() {
					return Health.down().build();
				}
			};
		}

		@Bean
		public DiscoveryCompositeHealthIndicator discoveryCompositeHealthIndicator(
				List<DiscoveryHealthIndicator> indicators) {
			return new DiscoveryCompositeHealthIndicator(new OrderedHealthAggregator(),
					indicators);
		}

	}

}
