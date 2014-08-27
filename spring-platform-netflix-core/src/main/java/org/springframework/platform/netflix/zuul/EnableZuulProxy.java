package org.springframework.platform.netflix.zuul;

import java.lang.annotation.*;

import org.springframework.context.annotation.Import;
import org.springframework.platform.netflix.eureka.EnableEurekaClient;

/**
 * @author Spencer Gibb
 */
@EnableEurekaClient
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ZuulProxyConfiguration.class)
public @interface EnableZuulProxy {
}
