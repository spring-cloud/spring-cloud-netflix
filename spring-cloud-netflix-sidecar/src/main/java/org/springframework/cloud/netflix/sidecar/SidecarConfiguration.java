package org.springframework.cloud.netflix.sidecar;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Spencer Gibb
 */
@Configuration
@EnableConfigurationProperties
@ConditionalOnExpression("${sidecar.enabled:true}")
public class SidecarConfiguration {

    @Bean
    public SidecarProperties sidecarProperties() {
        return new SidecarProperties();
    }

    @Bean
    public LocalApplicationHealthIndicator localApplicationHealthIndicator() {
        return new LocalApplicationHealthIndicator();
    }

    @Bean
    public SidecarController sidecarController() {
        return new SidecarController();
    }
}
