package org.springframework.cloud.netflix.ribbon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.cloud.netflix.ribbon.eureka.EurekaRibbonClientPreprocessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import com.netflix.loadbalancer.BaseLoadBalancer;

/**
 * @author Spencer Gibb
 */
@Configuration
@AutoConfigureAfter(EurekaClientAutoConfiguration.class)
public class RibbonAutoConfiguration {

	@Autowired(required = false)
	private List<BaseLoadBalancer> balancers = Collections.emptyList();

	@Autowired
	private EurekaRibbonClientPreprocessor clientPreprocessor;
	
	// TODO: need to find a default for this?
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
