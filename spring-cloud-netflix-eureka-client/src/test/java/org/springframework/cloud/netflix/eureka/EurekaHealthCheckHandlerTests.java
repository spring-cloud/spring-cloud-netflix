/*
 * Copyright 2013-2022 the original author or authors.
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.AbstractReactiveHealthIndicator;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.actuate.health.SimpleStatusAggregator;
import org.springframework.cloud.client.discovery.health.DiscoveryClientHealthIndicator;
import org.springframework.cloud.client.discovery.health.DiscoveryCompositeHealthContributor;
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
 * @author Olga Maciaszek-Sharma
 */
class EurekaHealthCheckHandlerTests {

	private EurekaHealthCheckHandler healthCheckHandler;

	@BeforeEach
	void setUp() {

		healthCheckHandler = new EurekaHealthCheckHandler(new SimpleStatusAggregator());
	}

	@Test
	void testNoHealthCheckRegistered() {

		InstanceStatus status = healthCheckHandler.getStatus(InstanceStatus.UNKNOWN);
		assertThat(status).isEqualTo(InstanceStatus.UNKNOWN);
	}

	@Test
	void testAllUp() {
		initialize(UpHealthConfiguration.class, ReactiveUpHealthConfiguration.class);

		InstanceStatus status = healthCheckHandler.getStatus(InstanceStatus.UNKNOWN);
		assertThat(status).isEqualTo(InstanceStatus.UP);
	}

	@Test
	void testHealthCheckNotReturnedWhenStopped() {
		initialize(UpHealthConfiguration.class);

		healthCheckHandler.stop();
		InstanceStatus status = healthCheckHandler.getStatus(InstanceStatus.UNKNOWN);

		assertThat(status).isNull();
		healthCheckHandler.start();
		InstanceStatus newStatus = healthCheckHandler.getStatus(InstanceStatus.UNKNOWN);
		assertThat(newStatus).isEqualTo(InstanceStatus.UP);
	}

	@Test
	void testDownWithBlockingIndicators() {
		initialize(UpHealthConfiguration.class, DownHealthConfiguration.class);

		InstanceStatus status = healthCheckHandler.getStatus(InstanceStatus.UNKNOWN);
		assertThat(status).isEqualTo(InstanceStatus.DOWN);
	}

	@Test
	void testDownWithReactiveIndicators() {
		initialize(UpHealthConfiguration.class, ReactiveDownHealthConfiguration.class);

		InstanceStatus status = healthCheckHandler.getStatus(InstanceStatus.UNKNOWN);
		assertThat(status).isEqualTo(InstanceStatus.DOWN);
	}

	@Test
	void testDownWhenBlockingIndicatorUpAndReactiveDown() {
		initialize(ReactiveUpHealthConfiguration.class, DownHealthConfiguration.class);

		InstanceStatus status = this.healthCheckHandler.getStatus(InstanceStatus.UNKNOWN);
		assertThat(status).isEqualTo(InstanceStatus.DOWN);
	}

	@Test
	void testDownWhenBlockingIndicatorDownAndReactiveUp() {
		initialize(ReactiveUpHealthConfiguration.class, ReactiveDownHealthConfiguration.class);

		InstanceStatus status = this.healthCheckHandler.getStatus(InstanceStatus.UNKNOWN);
		assertThat(status).isEqualTo(InstanceStatus.DOWN);
	}

	@Test
	void testUnknown() {
		initialize(FatalHealthConfiguration.class);

		InstanceStatus status = healthCheckHandler.getStatus(InstanceStatus.UNKNOWN);
		assertThat(status).isEqualTo(InstanceStatus.UNKNOWN);
	}

	@Test
	void testEurekaIgnored() {
		initialize(EurekaDownHealthConfiguration.class);

		InstanceStatus status = healthCheckHandler.getStatus(InstanceStatus.UP);
		assertThat(status).isEqualTo(InstanceStatus.UP);
	}

	@Test
	void testCompositeComponentsDown() {
		initialize(CompositeComponentsDownHealthConfiguration.class);

		InstanceStatus status = healthCheckHandler.getStatus(InstanceStatus.UP);
		assertThat(status).isEqualTo(InstanceStatus.DOWN);
	}

	@Test
	void testCompositeComponentsUp() {
		initialize(CompositeComponentsUpHealthConfiguration.class);

		InstanceStatus status = healthCheckHandler.getStatus(InstanceStatus.UP);
		assertThat(status).isEqualTo(InstanceStatus.UP);
	}

	@Test
	void testCompositeComponentsOneDown() {
		initialize(CompositeComponentsOneDownHealthConfiguration.class);

		InstanceStatus status = healthCheckHandler.getStatus(InstanceStatus.UP);
		assertThat(status).isEqualTo(InstanceStatus.DOWN);
	}

	private void initialize(Class<?>... configurations) {
		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(configurations);
		healthCheckHandler.setApplicationContext(applicationContext);
		healthCheckHandler.afterPropertiesSet();
	}

	public static class UpHealthConfiguration {

		@Bean
		public HealthIndicator healthIndicator() {
			return new AbstractHealthIndicator() {
				@Override
				protected void doHealthCheck(Health.Builder builder) {
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
				protected void doHealthCheck(Health.Builder builder) {
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
				protected void doHealthCheck(Health.Builder builder) {
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
		public DiscoveryCompositeHealthContributor discoveryCompositeHealthContributor(
				List<DiscoveryHealthIndicator> indicators) {
			return new DiscoveryCompositeHealthContributor(indicators);
		}

	}

	protected static class CompositeComponentsDownHealthConfiguration {

		@Bean
		public CompositeHealthContributor compositeHealthContributor() {
			return new TestCompositeHealthContributor(InstanceStatus.DOWN, InstanceStatus.DOWN);
		}

	}

	protected static class CompositeComponentsUpHealthConfiguration {

		@Bean
		public CompositeHealthContributor compositeHealthContributor() {
			return new TestCompositeHealthContributor(InstanceStatus.UP, InstanceStatus.UP);
		}

	}

	protected static class CompositeComponentsOneDownHealthConfiguration {

		@Bean
		public CompositeHealthContributor compositeHealthContributor() {
			return new TestCompositeHealthContributor(InstanceStatus.UP, InstanceStatus.DOWN);
		}

	}

	static class TestCompositeHealthContributor implements CompositeHealthContributor {

		private final Map<String, HealthContributor> contributorMap = new HashMap<>();

		TestCompositeHealthContributor(InstanceStatus firstContributorStatus, InstanceStatus secondContributorStatus) {
			contributorMap.put("first", new AbstractHealthIndicator() {
				@Override
				protected void doHealthCheck(Health.Builder builder) {
					builder.status(firstContributorStatus.name());
				}
			});
			contributorMap.put("second", new AbstractHealthIndicator() {
				@Override
				protected void doHealthCheck(Health.Builder builder) {
					builder.status(secondContributorStatus.name());
				}
			});
		}

		@Override
		public HealthContributor getContributor(String name) {
			return contributorMap.get(name);
		}

		@Override
		public Iterator<NamedContributor<HealthContributor>> iterator() {
			Iterator<Map.Entry<String, HealthContributor>> iterator = contributorMap.entrySet().iterator();
			return new Iterator<>() {

				@Override
				public boolean hasNext() {
					return iterator.hasNext();
				}

				@Override
				public NamedContributor<HealthContributor> next() {
					Map.Entry<String, HealthContributor> entry = iterator.next();
					return NamedContributor.of(entry.getKey(), entry.getValue());
				}

			};
		}

	}

}
