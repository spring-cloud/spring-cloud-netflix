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

package org.springframework.cloud.netflix.hystrix.amqp;

import javax.annotation.PostConstruct;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.hystrix.HystrixConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.amqp.Amqp;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.HystrixCircuitBreaker;

/**
 * @author Spencer Gibb
 */
@Configuration
@ConditionalOnClass({ HystrixCircuitBreaker.class, RabbitTemplate.class })
@ConditionalOnProperty(value = "hystrix.stream.amqp.enabled", matchIfMissing = true)
@IntegrationComponentScan(basePackageClasses = HystrixStreamChannel.class)
@EnableConfigurationProperties
@EnableScheduling
public class HystrixStreamAutoConfiguration {

	@Autowired
	private RabbitTemplate amqpTemplate;

	@Autowired(required = false)
	private ObjectMapper objectMapper;

	@PostConstruct
	public void init() {
		Jackson2JsonMessageConverter converter = messageConverter();
		this.amqpTemplate.setMessageConverter(converter);
	}

	@Bean
	public HystrixStreamAmqpProperties hystrixStreamAmqpProperties() {
		return new HystrixStreamAmqpProperties();
	}

	@Bean
	public HystrixStreamTask hystrixStreamTask() {
		return new HystrixStreamTask();
	}

	@Bean
	public DirectChannel hystrixStream() {
		return new DirectChannel();
	}

	@Bean
	public DirectExchange hystrixStreamExchange() {
		DirectExchange exchange = new DirectExchange(HystrixConstants.HYSTRIX_STREAM_NAME);
		return exchange;
	}

	@Bean
	public IntegrationFlow hystrixStreamOutboundFlow() {
		return IntegrationFlows
				.from("hystrixStream")
				// TODO: set content type
				/*
				 * .enrichHeaders(new ComponentConfigurer<HeaderEnricherSpec>() {
				 * 
				 * @Override public void configure(HeaderEnricherSpec spec) {
				 * spec.header("content-type", "application/json", true); } })
				 */
				.handle(Amqp.outboundAdapter(this.amqpTemplate).exchangeName(
						HystrixConstants.HYSTRIX_STREAM_NAME)).get();
	}

	private Jackson2JsonMessageConverter messageConverter() {
		Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
		if (this.objectMapper != null) {
			converter.setJsonObjectMapper(this.objectMapper);
		}
		return converter;
	}

}
