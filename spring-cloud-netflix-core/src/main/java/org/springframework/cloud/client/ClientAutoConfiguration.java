package org.springframework.cloud.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Spencer Gibb
 * TODO: move org.springframework.cloud.client to a spring-cloud-common project
 */
@Configuration
@EnableConfigurationProperties
@ConditionalOnExpression("${spring.cloud.client.enabled:true}")
public class ClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ClientProperties.class)
    public ClientProperties clientProperties() {
        return new ClientProperties();
    }

}
