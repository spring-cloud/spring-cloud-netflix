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

import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.aspectj.lang.JoinPoint;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.netflix.metrics.servo.ServoMonitorCache;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.netflix.servo.MonitorRegistry;
import com.netflix.servo.monitor.Monitors;

/**
 * @author Jon Schneider
 */
@Configuration
@ConditionalOnProperty(value = "spring.cloud.netflix.metrics.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass({ Monitors.class, MetricReader.class })
public class MetricsInterceptorConfiguration {

	@Configuration
	@ConditionalOnWebApplication
	@ConditionalOnClass(WebMvcConfigurerAdapter.class)
	static class MetricsWebResourceConfiguration extends WebMvcConfigurerAdapter {
		@Bean
		MetricsHandlerInterceptor servoMonitoringWebResourceInterceptor() {
			return new MetricsHandlerInterceptor();
		}

		@Override
		public void addInterceptors(InterceptorRegistry registry) {
			registry.addInterceptor(servoMonitoringWebResourceInterceptor());
		}
	}

	@Configuration
	@ConditionalOnClass({ RestTemplate.class, JoinPoint.class })
	@ConditionalOnProperty(value = "spring.aop.enabled", havingValue = "true", matchIfMissing = true)
	static class MetricsRestTemplateAspectConfiguration {

		@Bean
		RestTemplateUrlTemplateCapturingAspect restTemplateUrlTemplateCapturingAspect() {
			return new RestTemplateUrlTemplateCapturingAspect();
		}

	}

	@Configuration
	@ConditionalOnClass({ RestTemplate.class, HttpServletRequest.class })	// HttpServletRequest implicitly required by MetricsTagProvider
	static class MetricsRestTemplateConfiguration {

		@Value("${netflix.metrics.restClient.metricName:restclient}")
		String metricName;

		@Bean
		MetricsClientHttpRequestInterceptor spectatorLoggingClientHttpRequestInterceptor(
				Collection<MetricsTagProvider> tagProviders,
				ServoMonitorCache servoMonitorCache) {
			return new MetricsClientHttpRequestInterceptor(tagProviders,
					servoMonitorCache, this.metricName);
		}

		@Bean
		BeanPostProcessor spectatorRestTemplateInterceptorPostProcessor() {
			return new MetricsInterceptorPostProcessor();
		}

		private static class MetricsInterceptorPostProcessor
				implements BeanPostProcessor, ApplicationContextAware {
			private ApplicationContext context;
			private MetricsClientHttpRequestInterceptor interceptor;

			@Override
			public Object postProcessBeforeInitialization(Object bean, String beanName) {
				return bean;
			}

			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) {
				if (bean instanceof RestTemplate) {
					if (this.interceptor == null) {
						this.interceptor = this.context
								.getBean(MetricsClientHttpRequestInterceptor.class);
					}
					RestTemplate restTemplate = (RestTemplate) bean;
					// create a new list as the old one may be unmodifiable (ie Arrays.asList())
					ArrayList<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
					interceptors.add(interceptor);
					interceptors.addAll(restTemplate.getInterceptors());
					restTemplate.setInterceptors(interceptors);
				}
				return bean;
			}

			@Override
			public void setApplicationContext(ApplicationContext context)
					throws BeansException {
				this.context = context;
			}
		}
	}
}
