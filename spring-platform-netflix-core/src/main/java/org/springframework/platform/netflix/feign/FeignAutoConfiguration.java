package org.springframework.platform.netflix.feign;

import feign.Contract;
import feign.Logger;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.platform.netflix.archaius.ArchaiusAutoConfiguration;

/**
 * Created by sgibb on 7/3/14.
 */
@Configuration
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
}
