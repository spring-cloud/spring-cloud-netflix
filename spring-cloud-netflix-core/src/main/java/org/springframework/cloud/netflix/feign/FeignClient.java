package org.springframework.cloud.netflix.feign;

import java.lang.annotation.*;

/**
 * @author Spencer Gibb
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FeignClient {
	String value();
	boolean loadbalance() default true;
}
