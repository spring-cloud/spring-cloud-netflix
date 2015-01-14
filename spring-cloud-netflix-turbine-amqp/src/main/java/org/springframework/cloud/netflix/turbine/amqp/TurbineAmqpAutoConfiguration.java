package org.springframework.cloud.netflix.turbine.amqp;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

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
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cloud.netflix.Constants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.amqp.Amqp;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Spencer Gibb
 */
@Configuration
@ConditionalOnClass(AmqpTemplate.class)
@ConditionalOnExpression("${turbine.amqp.enabled:true}")
public class TurbineAmqpAutoConfiguration {

	@Autowired
	private ConnectionFactory connectionFactory;

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
	public DirectExchange hystrixStreamExchange() {
		DirectExchange exchange = new DirectExchange(Constants.HYSTRIX_STREAM_NAME);
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
		Queue queue = new Queue(Constants.HYSTRIX_STREAM_NAME, false, false, false, args);
		return queue;
	}

	@Bean
	public IntegrationFlow hystrixStreamAggregatorInboundFlow() {
		return IntegrationFlows
				.from(Amqp.inboundAdapter(this.connectionFactory, hystrixStreamQueue())
						.messageConverter(messageConverter()))
				.channel("hystrixStreamAggregator").get();
	}

	@Bean
	public Aggregator hystrixStreamAggregator() {
		return new Aggregator();
	}

	private Jackson2JsonMessageConverter messageConverter() {
		Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
		if (this.objectMapper != null) {
			converter.setJsonObjectMapper(this.objectMapper);
		}
		return converter;
	}

}
