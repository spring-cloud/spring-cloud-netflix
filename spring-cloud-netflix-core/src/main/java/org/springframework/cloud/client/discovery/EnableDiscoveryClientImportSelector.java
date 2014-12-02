package org.springframework.cloud.client.discovery;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author Spencer Gibb
 */
@Order(Ordered.LOWEST_PRECEDENCE - 100)
@Slf4j
public class EnableDiscoveryClientImportSelector implements DeferredImportSelector,
        BeanClassLoaderAware {

    private ClassLoader beanClassLoader;

    @Override
    public String[] selectImports(AnnotationMetadata metadata) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata
                .getAnnotationAttributes(EnableDiscoveryClient.class.getName(),
                        true));

        Assert.notNull(attributes, "No EnableDiscoveryClient attributes found. Is "
                + metadata.getClassName()
                + " annotated with @EnableDiscoveryClient?");

        // Find all possible auto configuration classes, filtering duplicates
        List<String> factories = new ArrayList<>(new LinkedHashSet<>(
                SpringFactoriesLoader.loadFactoryNames(EnableDiscoveryClient.class,
                        this.beanClassLoader)));

        if (factories.size() > 1) {
            String factory = factories.get(0);
            //there should only every be one DiscoveryClient
            log.warn("More than one implementation of @EnableDiscoveryClient.  Using {} out of available {}", factory, factories);
            factories = Collections.singletonList(factory);
        }

        return factories.toArray(new String[factories.size()]);

    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.beanClassLoader = classLoader;
    }

}
