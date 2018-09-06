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

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceConfiguration;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.netflix.hystrix.HystrixCircuitBreaker;

/**
 * Autoconfiguration for a Spring Cloud Hystrix on Spring Cloud Stream. Enabled by default
 * if spring-cloud-stream is on the classpath, and can be switched off with
 * <code>hystrix.stream.queue.enabled</code>. There are some high level configuration
 * options in {@link HystrixStreamProperties}. The binding name for Spring Cloud Stream is
 * {@link HystrixStreamClient#OUTPUT} so you can configure stream other properties through
 * that.
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
@AutoConfigureBefore(BindingServiceConfiguration.class) // Needed for bindings done in auto config
public class HystrixStreamAutoConfiguration {

	@Autowired
	private BindingServiceProperties bindings;

	@Autowired
	private HystrixStreamProperties properties;

	@Autowired
	@Output(HystrixStreamClient.OUTPUT)
	private MessageChannel outboundChannel;

	@Autowired(required = false)
	private Registration registration;

	@Bean
	public HasFeatures hystrixStreamQueueFeature() {
		return HasFeatures.namedFeature("Hystrix Stream (Queue)",
				HystrixStreamAutoConfiguration.class);
	}

	@PostConstruct
	public void init() {
		BindingProperties outputBinding = this.bindings.getBindings()
				.get(HystrixStreamClient.OUTPUT);
		if (outputBinding == null) {
			this.bindings.getBindings().put(HystrixStreamClient.OUTPUT,
					new BindingProperties());
		}
		BindingProperties output = this.bindings.getBindings()
				.get(HystrixStreamClient.OUTPUT);
		if (output.getDestination() == null) {
			output.setDestination(this.properties.getDestination());
		}
		if (output.getContentType() == null) {
			output.setContentType(this.properties.getContentType());
		}
	}

	@Bean
	public HystrixStreamProperties hystrixStreamProperties() {
		return new HystrixStreamProperties();
	}

	@Bean
	public HystrixStreamTask hystrixStreamTask(SimpleDiscoveryProperties simpleDiscoveryProperties) {
		ServiceInstance serviceInstance = this.registration;
		if (serviceInstance == null) {
			serviceInstance = simpleDiscoveryProperties.getLocal();
		}
		return new HystrixStreamTask(this.outboundChannel, serviceInstance,
				this.properties);
	}

}
