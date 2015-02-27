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

package org.springframework.cloud.netflix.turbine.amqp;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.hystrix.HystrixConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.amqp.Amqp;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Autoconfiguration for a Spring Cloud Turbine on AMQP. Enabled by default if
 * spring-rabbit is on the classpath, and can be switched off with
 * <code>spring.cloud.bus.amqp.enabled</code>. If there is a single
 * {@link ConnectionFactory} in the context it will be used, or if there is a one
 * qualified as <code>@TurbineConnectionFactory</code> it will be preferred over others,
 * otherwise the <code>@Primary</code> one will be used. If there are multiple unqualified
 * connection factories there will be an autowiring error. Note that Spring Boot (as of
 * 1.2.2) creates a ConnectionFactory that is <i>not</i> <code>@Primary</code>, so if you
 * want to use one connection factory for the bus and another for business messages, you
 * need to create both, and annotate them <code>@TurbineConnectionFactory</code> and
 * <code>@Primary</code> respectively.
 *
 * @author Spencer Gibb
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass(AmqpTemplate.class)
@ConditionalOnProperty(value = "turbine.amqp.enabled", matchIfMissing = true)
public class TurbineAmqpAutoConfiguration {

	@Autowired(required = false)
	@TurbineConnectionFactory
	private ConnectionFactory turbineConnectionFactory;

	@Autowired(required = false)
	private ConnectionFactory primaryConnectionFactory;

	@Autowired(required = false)
	private ObjectMapper objectMapper;

	private RabbitTemplate amqpTemplate;

	public RabbitTemplate amqpTemplate() {
		if (this.amqpTemplate == null) {
			RabbitTemplate amqpTemplate = new RabbitTemplate(connectionFactory());
			Jackson2JsonMessageConverter converter = messageConverter();
			amqpTemplate.setMessageConverter(converter);
			this.amqpTemplate = amqpTemplate;
		}
		return this.amqpTemplate;
	}

	@Bean
	public DirectExchange hystrixStreamExchange() {
		DirectExchange exchange = new DirectExchange(HystrixConstants.HYSTRIX_STREAM_NAME);
		return exchange;
	}

	@Bean
	protected Binding localTurbineAmqpQueueBinding() {
		return BindingBuilder.bind(hystrixStreamQueue()).to(hystrixStreamExchange())
				.with("");
	}

	@Bean
	public Queue hystrixStreamQueue() {
		Map<String, Object> args = new HashMap<>();
		args.put("x-message-ttl", 60000); // TODO: configure TTL
		Queue queue = new Queue(HystrixConstants.HYSTRIX_STREAM_NAME, false, false,
				false, args);
		return queue;
	}

	@Bean
	public IntegrationFlow hystrixStreamAggregatorInboundFlow() {
		return IntegrationFlows
				.from(Amqp.inboundAdapter(connectionFactory(), hystrixStreamQueue())
						.messageConverter(messageConverter()))
				.channel("hystrixStreamAggregator").get();
	}

	@Bean
	public Aggregator hystrixStreamAggregator() {
		return new Aggregator();
	}

	private ConnectionFactory connectionFactory() {
		if (this.turbineConnectionFactory != null) {
			return this.turbineConnectionFactory;
		}
		return this.primaryConnectionFactory;
	}

	private Jackson2JsonMessageConverter messageConverter() {
		Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
		if (this.objectMapper != null) {
			converter.setJsonObjectMapper(this.objectMapper);
		}
		return converter;
	}

}
