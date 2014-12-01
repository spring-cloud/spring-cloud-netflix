package org.springframework.cloud.netflix.feign;

import feign.Client;
import feign.Contract;
import feign.Feign;
import feign.Logger;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.netflix.feign.ribbon.FeignRibbonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration;

import com.netflix.loadbalancer.ILoadBalancer;

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
        //return new Slf4jLogger(); //TODO pass Client classname in
        return new Logger.JavaLogger();
    }

    @Bean
    public Contract feignContract() {
        return new SpringMvcContract();
    }

    @Bean
    @ConditionalOnClass(ILoadBalancer.class)
    public Client feignRibbonClient() { return new FeignRibbonClient(); }
}
