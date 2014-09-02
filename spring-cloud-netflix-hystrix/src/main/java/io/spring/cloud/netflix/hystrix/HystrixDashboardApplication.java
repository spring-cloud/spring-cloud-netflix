package io.spring.cloud.netflix.hystrix;

import io.spring.cloud.netflix.hystrix.stream.MockStreamServlet;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.cloud.netflix.hystrix.annotations.EnableHystrixDashboard;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created by sgibb on 7/11/14.
 */
@Configuration
@ComponentScan
@EnableHystrixDashboard
@EnableAutoConfiguration
public class HystrixDashboardApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(HystrixDashboardApplication.class).web(true);
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(HystrixDashboardApplication.class).web(true).run(args);
    }

    @Bean
    public ServletRegistrationBean mockStreamServlet() {
        return new ServletRegistrationBean(new MockStreamServlet(), "/mock.stream");
    }

}
