package org.springframework.netflix.hystrix.amqp;

import com.netflix.hystrix.HystrixCircuitBreaker;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cloud.netflix.Constants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.amqp.Amqp;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author Spencer Gibb
 */
@Configuration
@ConditionalOnClass({HystrixCircuitBreaker.class, AmqpTemplate.class})
public class HystrixStreamAutoConfiguration {
    @Configuration
    @ConditionalOnExpression("${hystrix.stream.amqp.enabled:true}")
    @IntegrationComponentScan(basePackageClasses = HystrixStreamChannel.class)
    @EnableScheduling
    protected static class HystrixStreamAmqpAutoConfiguration {

        @Autowired
        private AmqpTemplate amqpTemplate;

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
            return IntegrationFlows.from("hystrixStream")
                    //TODO: set content type
                    /*.enrichHeaders(new ComponentConfigurer<HeaderEnricherSpec>() {
                        @Override
                        public void configure(HeaderEnricherSpec spec) {
                            spec.header("content-type", "application/json", true);
                        }
                    })*/
                    .handle(Amqp.outboundAdapter(this.amqpTemplate).exchangeName(Constants.HYSTRIX_STREAM_NAME))
                    .get();
        }
    }

}
