package org.springframework.cloud.netflix.feign;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * @author Eko Kurniawan Khannedy
 */
class FeignClientHealthRegistrar {

	public void registerFeignClientHealthIndicator(BeanDefinitionRegistry registry,
																								 String feignClass,
																								 String feignName,
																								 String healthMethod) {

		String name = feignName + "HealthIndicator";
		BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(FeignClientHealthIndicator.class);
		definition.addPropertyValue("feignClass", feignClass);
		definition.addPropertyValue("healthMethod", healthMethod);
		definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
		AbstractBeanDefinition beanDefinition = definition.getBeanDefinition();

		BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, name, new String[]{});
		BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
	}

}
