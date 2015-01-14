package org.springframework.cloud.netflix.zuul;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

/**
 * Sets up a Zuul server endpoint and installs some reverse proxy filters in it, so it can
 * forward requests to backend servers. The backends can be registered manually through
 * configuration or via Eureka.
 *
 * @see EnableZuulServer for how to get a Zuul server without any proxying
 *
 * @author Spencer Gibb
 * @author Dave Syer
 */
@EnableCircuitBreaker
@EnableDiscoveryClient
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(ZuulProxyConfiguration.class)
public @interface EnableZuulProxy {
}
