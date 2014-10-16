package org.springframework.cloud.netflix.ribbon;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.cloud.netflix.ribbon.eureka.EurekaRibbonInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import com.netflix.loadbalancer.BaseLoadBalancer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Spencer Gibb
 */
@Configuration
@AutoConfigureAfter(EurekaClientAutoConfiguration.class)
public class RibbonAutoConfiguration {
	
	@Autowired(required=false)
	private List<BaseLoadBalancer> balancers = Collections.emptyList();

	@Autowired(required=false)
	private EurekaRibbonInitializer initializer;

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
        return new RibbonLoadBalancerClient(balancers);
    }

    @Bean
    public RibbonInterceptor ribbonInterceptor(LoadBalancerClient loadBalancerClient) {
        return new RibbonInterceptor(loadBalancerClient);
    }

    @Bean
    public ServerListInitializer serverListInitializer() {
        return new ServerListInitializer() {
            @Override
            public void initialize(String serviceId) {
                //no-op
            }
        };
    }
}
