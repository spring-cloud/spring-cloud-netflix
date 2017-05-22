/*
 * Copyright 2013-2017 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

/**
 * @author Eko Kurniawan Khannedy
 */
@Configuration
@ConditionalOnClass({Endpoint.class})
class FeignClientHealthConfiguration {

	@Bean
	public FeignClientHealthRegistrar feignClientHealthRegistrar() {
		return new FeignClientHealthRegistrar();
	}

	class FeignClientHealthRegistrar {

		public void register(BeanDefinitionRegistry registry, String feignClass, String feignName, String healthMethod) {

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

	class FeignClientHealthIndicator extends AbstractHealthIndicator implements ApplicationContextAware {

		private ApplicationContext applicationContext;

		private Class<?> feignClass;

		private String healthMethod;

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
			this.applicationContext = applicationContext;
		}

		public void setFeignClass(String feignClass) throws ClassNotFoundException {
			this.feignClass = Class.forName(feignClass);
		}

		public void setHealthMethod(String healthMethod) {
			this.healthMethod = healthMethod;
		}

		@Override
		protected void doHealthCheck(Health.Builder builder) throws Exception {
			Object feign = applicationContext.getBean(feignClass);
			Method method = ReflectionUtils.findMethod(feignClass, healthMethod);

			try {
				Object result = ReflectionUtils.invokeMethod(method, feign);
				if (result instanceof ResponseEntity) {
					ResponseEntity responseEntity = (ResponseEntity) result;
					if (responseEntity.getStatusCodeValue() == HttpStatus.OK.value()) {
						builder.up();
					} else {
						builder.down();
					}

					builder.withDetail("statusCode", responseEntity.getStatusCode().value())
							.withDetail("responseBody", responseEntity.getBody());
				} else {
					builder.up()
							.withDetail("responseBody", result);
				}
			} catch (Exception ex) {
				builder.down(ex);
			} catch (Throwable throwable) {
				builder.down().withException(new Exception(throwable));
			}
		}
	}


}
