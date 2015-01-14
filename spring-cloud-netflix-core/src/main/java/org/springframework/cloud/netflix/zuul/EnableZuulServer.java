package org.springframework.cloud.netflix.zuul;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * Set up the application to act as a generic Zuul server without any built-in reverse
 * proxy features. The routes into the Zuul server can be configured through
 * {@link ZuulProperties} (by default there are none).
 *
 * @see EnableZuulProxy to see how to get reverse proxy out of the box
 *
 * @author Spencer Gibb
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ZuulConfiguration.class)
public @interface EnableZuulServer {
}
