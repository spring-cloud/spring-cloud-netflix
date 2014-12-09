package org.springframework.cloud.netflix.ribbon;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import com.netflix.client.IClient;

/**
 * Auto configuration for Ribbon (client side load balancing)
 * @author Spencer Gibb
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass(IClient.class)
@RibbonClients
@AutoConfigureAfter(EurekaClientAutoConfiguration.class)
public class RibbonAutoConfiguration {

	@Autowired(required=false)
    private List<RibbonClientSpecification> configurations = new ArrayList<>();

	@Bean
    public SpringClientFactory springClientFactory() {
        SpringClientFactory factory = new SpringClientFactory();
        factory.setConfigurations(configurations);
		return factory;
    }

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
		return new RibbonLoadBalancerClient(springClientFactory());
	}

	@Bean
	public RibbonInterceptor ribbonInterceptor(LoadBalancerClient loadBalancerClient) {
		return new RibbonInterceptor(loadBalancerClient);
	}

}
