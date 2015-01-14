package org.springframework.netflix.hystrix.amqp;

import javax.annotation.PostConstruct;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.Constants;
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
@ConditionalOnExpression("${hystrix.stream.amqp.enabled:true}")
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
		DirectExchange exchange = new DirectExchange(Constants.HYSTRIX_STREAM_NAME);
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
						Constants.HYSTRIX_STREAM_NAME)).get();
	}

	private Jackson2JsonMessageConverter messageConverter() {
		Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
		if (this.objectMapper != null) {
			converter.setJsonObjectMapper(this.objectMapper);
		}
		return converter;
	}

}
