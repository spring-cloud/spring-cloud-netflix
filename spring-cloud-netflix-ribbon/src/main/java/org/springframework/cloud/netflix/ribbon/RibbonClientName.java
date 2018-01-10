package org.springframework.cloud.netflix.ribbon;

import org.springframework.beans.factory.annotation.Value;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation at the field or method/constructor parameter level that injects the
 * Ribbon Client Name that got allocated at runtime. Provides a convenient
 * alternative for <code>&#064;Value(&quot;${ribbon.client.name}&quot;)</code>.
 *
 * @author Spencer Gibb
 * @since 2.0.0
 */
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER,
        ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Value("${ribbon.client.name}")
public @interface RibbonClientName {

}
