/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.cloud.netflix.metrics;

import org.aspectj.lang.JoinPoint;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.netflix.servo.monitor.Monitors;

/**
 * @author Jon Schneider
 */
@Configuration
@ConditionalOnClass({ Monitors.class, MetricReader.class })
public class MetricsInterceptorConfiguration {

	@Configuration
	@ConditionalOnWebApplication
	static class MetricsWebResourceConfiguration extends WebMvcConfigurerAdapter {
		@Bean
		MetricsHandlerInterceptor spectatorMonitoringWebResourceInterceptor() {
			return new MetricsHandlerInterceptor();
		}

		@Override
		public void addInterceptors(InterceptorRegistry registry) {
			registry.addInterceptor(spectatorMonitoringWebResourceInterceptor());
		}
	}

	@Configuration
	@ConditionalOnClass(JoinPoint.class)
	@ConditionalOnProperty(value = "spring.aop.enabled", havingValue = "true", matchIfMissing = true)
	@ConditionalOnBean({ RestTemplate.class })
	static class MetricsRestTemplateAspectConfiguration {

		@Bean
		RestTemplateUrlTemplateCapturingAspect restTemplateUrlTemplateCapturingAspect() {
			return new RestTemplateUrlTemplateCapturingAspect();
		}

	}

	@Configuration
	@ConditionalOnBean({ RestTemplate.class })
	static class MetricsRestTemplateConfiguration {

		@Bean
		MetricsClientHttpRequestInterceptor spectatorLoggingClientHttpRequestInterceptor() {
			return new MetricsClientHttpRequestInterceptor();
		}

		@Bean
		BeanPostProcessor spectatorRestTemplateInterceptorPostProcessor(
				final MetricsClientHttpRequestInterceptor interceptor) {
			return new BeanPostProcessor() {
				@Override
				public Object postProcessBeforeInitialization(Object bean,
						String beanName) {
					return bean;
				}

				@Override
				public Object postProcessAfterInitialization(Object bean,
						String beanName) {
					if (bean instanceof RestTemplate) {
						((RestTemplate) bean).getInterceptors().add(interceptor);
					}
					return bean;
				}
			};
		}
	}
}
