package org.springframework.cloud.netflix.zuul;

import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author Spencer Gibb
 * @deprecated @see org.springframework.cloud.netflix.zuul.EnableZuulProxy
 */
@EnableHystrix
@EnableEurekaClient
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ZuulConfiguration.class)
public @interface EnableZuulServer {
}
