package org.springframework.cloud.netflix.ribbon;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Spencer Gibb
 */
@Configuration
@AutoConfigureAfter(EurekaClientAutoConfiguration.class)
public class RibbonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RestTemplate.class)
    public RestTemplate restTemplate(RibbonInterceptor ribbonInterceptor) {
        RestTemplate restTemplate = new RestTemplate();
        List<ClientHttpRequestInterceptor> list = new ArrayList<>();
        list.add(ribbonInterceptor);
        restTemplate.setInterceptors(list);
        return restTemplate;
    }

    @Bean
    @ConditionalOnMissingBean(LoadBalancerClient.class)
    public LoadBalancerClient loadBalancerClient() {
        return new RibbonLoadBalancerClient();
    }

    @Bean
    public RibbonInterceptor ribbonInterceptor(LoadBalancerClient loadBalancerClient) {
        return new RibbonInterceptor(loadBalancerClient);
    }
}
