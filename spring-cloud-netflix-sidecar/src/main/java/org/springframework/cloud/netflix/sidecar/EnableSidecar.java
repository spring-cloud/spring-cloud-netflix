package org.springframework.cloud.netflix.sidecar;

import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author Spencer Gibb
 */
@EnableHystrix
@EnableEurekaClient
@EnableZuulProxy
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(SidecarConfiguration.class)
public @interface EnableSidecar {
}
