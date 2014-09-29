package org.springframework.cloud.netflix.ribbon;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaRibbonInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Spencer Gibb
 */
@Configuration
@AutoConfigureAfter(EurekaClientAutoConfiguration.class)
public class RibbonAutoConfiguration {

    //TODO: why doesn't @AutoConfigureAfter(EurekaClientAutoConfiguration.class) do what the following does for order?
    @Autowired
    EurekaRibbonInitializer eurekaRibbonInitializer;

    @Bean
    @ConditionalOnMissingBean(RestTemplate.class)
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public RibbonInterceptor ribbonInterceptor() {
        return new RibbonInterceptor();
    }

    @PostConstruct
    public void init() {
        List<ClientHttpRequestInterceptor> list = new ArrayList<>();
        list.add(ribbonInterceptor());
        restTemplate().setInterceptors(list);
    }
}
