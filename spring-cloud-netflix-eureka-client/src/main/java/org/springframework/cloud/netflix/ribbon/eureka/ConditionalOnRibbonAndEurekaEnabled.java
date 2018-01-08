package org.springframework.cloud.netflix.ribbon.eureka;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Igor Kryvenko
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(RibbonEurekaAutoConfiguration.OnRibbonAndEurekaEnabledCondition.class)
public @interface ConditionalOnRibbonAndEurekaEnabled {

}
