package org.springframework.cloud.netflix.hystrix;

/**
 * @author Spencer Gibb
 */

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;

/**
 * Convenience annotation for clients to enable Hystrix circuit breakers (specifically).
 * Use this (optionally) in case you want discovery and know for sure that it is Hystrix
 * you want. All it does is turn on circuit breakers and let the autoconfiguration find
 * the Hystrix classes if they are available (i.e. you need Hystrix on the classpath as
 * well).
 *
 * @author Dave Syer
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@EnableCircuitBreaker
public @interface EnableHystrix {
}
