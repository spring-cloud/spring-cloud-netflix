package org.springframework.cloud.netflix.zuul;

import java.lang.annotation.*;

import org.springframework.context.annotation.Import;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;

/**
 * @author Spencer Gibb
 */
@EnableHystrix
@EnableEurekaClient
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(ZuulConfiguration.class)
public @interface EnableZuulProxy {
}
