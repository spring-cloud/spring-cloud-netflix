package org.springframework.cloud.netflix.hystrix.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.cloud.netflix.hystrix.dashboard.HystrixDashboardConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Created by sgibb on 6/19/14.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(HystrixDashboardConfiguration.class)
public @interface EnableHystrixDashboard {
}