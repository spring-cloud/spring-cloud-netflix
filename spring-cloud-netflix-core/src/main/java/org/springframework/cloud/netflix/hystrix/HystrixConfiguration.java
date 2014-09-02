package org.springframework.cloud.netflix.hystrix;

import com.netflix.hystrix.contrib.javanica.aop.aspectj.HystrixCommandAspect;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.cloud.netflix.hystrix.annotations.EnableHystrix;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.Collection;

/**
 * Created by sgibb on 6/19/14.
 */
@Configuration
public class HystrixConfiguration implements ImportAware {

    private AnnotationAttributes enableHystrix;

    @Bean
    HystrixCommandAspect hystrixCommandAspect() {
        return new HystrixCommandAspect();
    }

    @Bean
    //TODO: add enable/disable
    public HystrixStreamEndpoint hystrixStreamEndpoint() {
        return new HystrixStreamEndpoint();
    }

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        this.enableHystrix = AnnotationAttributes.fromMap(
                importMetadata.getAnnotationAttributes(EnableHystrix.class.getName(), false));
        Assert.notNull(this.enableHystrix,
                "@EnableHystrix is not present on importing class " + importMetadata.getClassName());
    }

    @Autowired(required=false)
    void setConfigurers(Collection<HystrixConfigurer> configurers) {
        if (CollectionUtils.isEmpty(configurers)) {
            return;
        }
        if (configurers.size() > 1) {
            throw new IllegalStateException("Only one TransactionManagementConfigurer may exist");
        }
        //TODO: create CircuitBreakerConfigurer API
        // CircuitBreakerConfigurer configurer = configurers.iterator().next();
        //this.txManager = configurer.annotationDrivenTransactionManager();
    }
}
