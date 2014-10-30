package org.springframework.cloud.netflix.ribbon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import com.netflix.loadbalancer.BaseLoadBalancer;

/**
 * Auto configuration for Ribbon (client side load balancing)
 * @author Spencer Gibb
 * @author Dave Syer
 */
@Configuration
@ConditionalOnBean(SpringClientFactory.class)
@AutoConfigureAfter(EurekaClientAutoConfiguration.class)
public class RibbonAutoConfiguration {

	@Autowired(required = false)
	private List<BaseLoadBalancer> balancers = Collections.emptyList();

	@Autowired
	private RibbonClientPreprocessor clientPreprocessor;
	
	@Autowired
	private SpringClientFactory springClientFactory;

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
		return new RibbonLoadBalancerClient(clientPreprocessor, springClientFactory, balancers);
	}

	@Bean
	public RibbonInterceptor ribbonInterceptor(LoadBalancerClient loadBalancerClient) {
		return new RibbonInterceptor(loadBalancerClient);
	}

	@Configuration
	protected static class DefaultRibbonClientPreprocessor {

		@Bean
		@ConditionalOnMissingBean(RibbonClientPreprocessor.class)
		public RibbonClientPreprocessor ribbonClientPreprocessor() {
			return new RibbonClientPreprocessor() {
				@Override
				public void preprocess(String serviceId) {
					// no-op
				}
			};
		}

	}
}
