package org.springframework.cloud.netflix.zuul;

import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author Spencer Gibb
 * @deprecated @see org.springframework.cloud.netflix.zuul.EnableZuulProxy
 */
@EnableCircuitBreaker
@EnableDiscoveryClient
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ZuulConfiguration.class)
public @interface EnableZuulServer {
}
