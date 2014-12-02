package org.springframework.cloud.netflix.sidecar;

import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author Spencer Gibb
 */
@EnableCircuitBreaker
@EnableDiscoveryClient
@EnableZuulProxy
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(SidecarConfiguration.class)
public @interface EnableSidecar {
}
