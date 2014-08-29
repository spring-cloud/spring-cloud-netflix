package io.spring.platform.netflix.zuul;

import com.netflix.zuul.context.ContextLifecycleFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.platform.netflix.endpoint.HystrixStreamEndpoint;
import org.springframework.platform.netflix.zuul.Routes;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.ArrayList;
import java.util.Collection;

/**
 * User: spencergibb
 * Date: 4/24/14
 * Time: 8:57 PM
 */
@Configuration
@EnableAutoConfiguration
@EnableScheduling
@ComponentScan(basePackageClasses = Application.class)
public class Application {

    @Bean
    ZuulProperties routerProperties() {
        return new ZuulProperties();
    }

    @Bean
    public FilterRegistrationBean contextLifecycleFilter() {
        Collection<String> urlPatterns = new ArrayList<>();
        urlPatterns.add("/*");

        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(new ContextLifecycleFilter());
        filterRegistrationBean.setUrlPatterns(urlPatterns);

        return filterRegistrationBean;
    }

    //TODO: replace with auto config
    @Bean
    HystrixStreamEndpoint hystrixStreamEndpoint() {
        return new HystrixStreamEndpoint();
    }

    @Bean
    Routes routes() {
        return new Routes();
    }

    @Bean
    FilterIntializer filterIntializer() {
        return new FilterIntializer();
    }

    @Bean
    ZuulController zuulController() {
        return new ZuulController();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
