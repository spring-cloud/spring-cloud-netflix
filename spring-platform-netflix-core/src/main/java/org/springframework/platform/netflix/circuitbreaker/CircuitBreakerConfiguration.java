package org.springframework.platform.netflix.circuitbreaker;

import com.netflix.hystrix.contrib.javanica.aop.aspectj.HystrixCommandAspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.platform.netflix.circuitbreaker.annotations.EnableCircuitBreaker;
import org.springframework.platform.netflix.endpoint.HystrixStreamEndpoint;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.Collection;

/**
 * Created by sgibb on 6/19/14.
 */
@Configuration
public class CircuitBreakerConfiguration implements ImportAware {

    private AnnotationAttributes enableCircuitBreaker;

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
        this.enableCircuitBreaker = AnnotationAttributes.fromMap(
                importMetadata.getAnnotationAttributes(EnableCircuitBreaker.class.getName(), false));
        Assert.notNull(this.enableCircuitBreaker,
                "@EnableCircuitBreaker is not present on importing class " + importMetadata.getClassName());
    }

    @Autowired(required=false)
    void setConfigurers(Collection<CircuitBreakerConfigurer> configurers) {
        if (CollectionUtils.isEmpty(configurers)) {
            return;
        }
        if (configurers.size() > 1) {
            throw new IllegalStateException("Only one TransactionManagementConfigurer may exist");
        }
        //TODO: create CircuitBreakerConfigurer API
        CircuitBreakerConfigurer configurer = configurers.iterator().next();
        //this.txManager = configurer.annotationDrivenTransactionManager();
    }
}
