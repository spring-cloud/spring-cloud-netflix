package org.springframework.platform.netflix.zuul;

import java.lang.annotation.*;

import org.springframework.context.annotation.Import;

/**
 * @author Spencer Gibb
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ZuulProxyConfiguration.class)
public @interface EnableZuulProxy {
}
