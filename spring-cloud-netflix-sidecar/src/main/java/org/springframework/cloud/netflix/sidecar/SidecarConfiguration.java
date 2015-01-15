package org.springframework.cloud.netflix.sidecar;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Spencer Gibb
 */
@Configuration
@EnableConfigurationProperties
@ConditionalOnProperty(value = "sidecar.enabled", matchIfMissing = true)
public class SidecarConfiguration {
    @Value("${server.port:${SERVER_PORT:${PORT:8080}}}")
    private int serverPort = 8080;

    @Bean
    public SidecarProperties sidecarProperties() {
        return new SidecarProperties();
    }

    @Bean
    public EurekaInstanceConfigBean eurekaInstanceConfigBean() {
        EurekaInstanceConfigBean config = new EurekaInstanceConfigBean();

        int port = sidecarProperties().getPort();
        config.setNonSecurePort(port);

        String scheme = config.getSecurePortEnabled()? "https" : "http";

        config.setStatusPageUrl(scheme + "://" + config.getHostname() + ":" + serverPort + config.getStatusPageUrlPath());
        config.setHealthCheckUrl(scheme + "://" + config.getHostname() + ":" + serverPort + config.getHealthCheckUrlPath());

        config.setHomePageUrl(scheme + "://" + config.getHostname() + ":" + port + config.getHomePageUrlPath());
        return config;
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
