package org.springframework.cloud.netflix.turbine.amqp;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * Run the RxNetty based Spring Cloud Turbine AMQP server. Based on Netflix Turbine 2
 *
 * @author Spencer Gibb
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(TurbineAmqpConfiguration.class)
public @interface EnableTurbineAmqp {
}
