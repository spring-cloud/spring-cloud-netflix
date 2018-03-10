/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.feign;

import org.apache.commons.lang.ArrayUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.cloud.context.named.NamedContextFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * A factory that creates instances of feign classes. It creates a Spring
 * ApplicationContext per client name, and extracts the beans that it needs from there.
 *
 * @author Spencer Gibb
 * @author Dave Syer
 */
public class FeignContext extends NamedContextFactory<FeignClientSpecification> {

    public FeignContext() {
        super(FeignClientsConfiguration.class, "feign", "feign.client.name");
    }

    /**
     * getSuitableInstance from context
     *
     * 1.without bean for the type return null
     * 2.only exist one return the bean
     * 3.choose the one which has @Primary annotation
     * 4.choose the one which name is FeignProducerName+type.getSimpleName
     * 5.on enough info for choose from multiple bean ,return null
     * @param name
     * @param type
     * @param <T>
     * @return
     */
    public <T> T getSuitableInstance(String name, Class<T> type) {
        AnnotationConfigApplicationContext context = this.getContext(name);

        String[] beanNames = context.getBeanNamesForType(type);

        if (ArrayUtils.isEmpty(beanNames)) {
            return null;
        }

        if (beanNames.length == 1) {
            return context.getBean(beanNames[0], type);
        }

        boolean defineServiceBean = false;
        String defineServiceName = name + type.getSimpleName();
        for (String beanName : beanNames) {
            BeanDefinition beanDefinition = context.getBeanDefinition(beanName);
            if (beanDefinition.isPrimary()) {
                return context.getBean(beanName, type);
            }
            if (beanName.equals(defineServiceName)) {
                defineServiceBean = true;
            }
        }

        return defineServiceBean ? context.getBean(defineServiceName, type) : null;
    }

}
