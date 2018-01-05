package org.springframework.cloud.netflix.ribbon.eureka;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

/**
 * @author Igor Kryvenko
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(RibbonEurekaAutoConfiguration.OnRibbonAndEurekaEnabledCondition.class)
public @interface ConditionalOnRibbonAndEurekaEnabled {

}
