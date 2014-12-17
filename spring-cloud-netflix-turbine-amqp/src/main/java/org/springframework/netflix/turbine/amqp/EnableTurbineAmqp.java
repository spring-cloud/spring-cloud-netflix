package org.springframework.netflix.turbine.amqp;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Run the RxNetty based Spring Cloud Bus Turbine server.
 * Based on Netflix Turbine 2
 *
 * @author Spencer Gibb
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(TurbineAmqpConfiguration.class)
public @interface EnableTurbineAmqp {
}
