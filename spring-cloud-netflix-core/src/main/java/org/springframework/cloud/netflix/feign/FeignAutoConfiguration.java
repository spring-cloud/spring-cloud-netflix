package org.springframework.cloud.netflix.feign;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration;
import org.springframework.cloud.netflix.feign.ribbon.FeignRibbonClient;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.loadbalancer.ILoadBalancer;

import feign.Client;
import feign.Contract;
import feign.Feign;
import feign.Logger;

/**
 * @author Spencer Gibb
 * @author Julien Roy
 */
@Configuration
@ConditionalOnClass(Feign.class)
@AutoConfigureAfter(ArchaiusAutoConfiguration.class)
public class FeignAutoConfiguration {
	@Bean
	SpringDecoder feignDecoder() {
		return new SpringDecoder();
	}

	@Bean
	SpringEncoder feignEncoder() {
		return new SpringEncoder();
	}

	@Bean
	public Logger feignLogger() {
		// return new Slf4jLogger(); //TODO pass Client classname in
		return new Logger.JavaLogger();
	}

	@Bean
	public Contract feignContract() {
		return new SpringMvcContract();
	}

	@ConditionalOnClass(ILoadBalancer.class)
	@Configuration
	protected static class RibbonClientConfiguration {
		@Bean
		public Client feignRibbonClient(SpringClientFactory factory) {
			return new FeignRibbonClient(factory);
		}
	}
}
