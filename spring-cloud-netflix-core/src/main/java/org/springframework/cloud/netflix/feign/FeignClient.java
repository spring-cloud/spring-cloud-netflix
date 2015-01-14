package org.springframework.cloud.netflix.feign;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Spencer Gibb
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FeignClient {
	/**
	 * @return serviceId if loadbalance is true, url otherwise No need to prefix serviceId
	 * with http://
	 */
	String value();

	boolean loadbalance() default true;
}
