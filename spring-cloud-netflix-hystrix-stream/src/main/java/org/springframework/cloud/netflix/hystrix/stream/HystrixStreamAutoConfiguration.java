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

package org.springframework.cloud.netflix.hystrix.stream;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.netflix.hystrix.HystrixCircuitBreaker;

/**
 * Autoconfiguration for a Spring Cloud Hystrix on AMQP. Enabled by default if
 * spring-rabbit is on the classpath, and can be switched off with
 * <code>spring.cloud.bus.amqp.enabled</code>. If there is a single
 * {@link ConnectionFactory} in the context it will be used, or if there is a one
 * qualified as <code>@HystrixConnectionFactory</code> it will be preferred over others,
 * otherwise the <code>@Primary</code> one will be used. If there are multiple unqualified
 * connection factories there will be an autowiring error. Note that Spring Boot (as of
 * 1.2.2) creates a ConnectionFactory that is <i>not</i> <code>@Primary</code>, so if you
 * want to use one connection factory for the bus and another for business messages, you
 * need to create both, and annotate them <code>@HystrixConnectionFactory</code> and
 * <code>@Primary</code> respectively.
 *
 * @author Spencer Gibb
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass({ HystrixCircuitBreaker.class, EnableBinding.class })
@ConditionalOnProperty(value = "hystrix.stream.queue.enabled", matchIfMissing = true)
@EnableConfigurationProperties
@EnableScheduling
@EnableBinding(HystrixStreamClient.class)
public class HystrixStreamAutoConfiguration {

	@Bean
	public HystrixStreamProperties hystrixStreamProperties() {
		return new HystrixStreamProperties();
	}

	@Bean
	public HystrixStreamTask hystrixStreamTask() {
		return new HystrixStreamTask();
	}

}
