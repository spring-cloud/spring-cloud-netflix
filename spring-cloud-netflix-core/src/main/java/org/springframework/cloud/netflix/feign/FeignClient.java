package org.springframework.cloud.netflix.feign;

import java.lang.annotation.*;

/**
 * Annotation for interfaces declaring that a REST client with that interface should be
 * created (e.g. for autowiring into another component).
 * @author Spencer Gibb
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FeignClient {
	/**
	 * @return serviceId if loadbalance is true, url otherwise There is no need to prefix
	 * serviceId with http://.
	 */
	String value();

	/**
	 * @return true if calls should be load balanced (assuming a load balancer is
	 * available).
	 */
	boolean loadbalance() default true;
}
