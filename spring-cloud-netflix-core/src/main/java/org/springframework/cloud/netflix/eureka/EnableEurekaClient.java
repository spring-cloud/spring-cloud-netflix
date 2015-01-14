package org.springframework.cloud.netflix.eureka;

/**
 * @author Spencer Gibb
 */

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Convenience annotation for clients to enable Eureka discovery configuration
 * (specifically). Use this (optionally) in case you want discovery and know for sure that
 * it is Eureka you want. All it does is turn on discovery and let the autoconfiguration
 * find the eureka classes if they are available (i.e. you need Eureka on the classpath as
 * well).
 *
 * @author Dave Syer
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@EnableDiscoveryClient
public @interface EnableEurekaClient {
}
