package org.springframework.cloud.netflix.metrics;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
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
	@ConditionalOnBean({ RestTemplate.class })
	static class MetricsRestTemplateConfiguration {
		@Bean
		RestTemplateUrlTemplateCapturingAspect restTemplateUrlTemplateCapturingAspect() {
			return new RestTemplateUrlTemplateCapturingAspect();
		}

		@Bean
		MetricsClientHttpRequestInterceptor spectatorLoggingClientHttpRequestInterceptor() {
			return new MetricsClientHttpRequestInterceptor();
		}

		@Bean
		BeanPostProcessor spectatorRestTemplateInterceptorPostProcessor(
				final MetricsClientHttpRequestInterceptor interceptor) {
			return new BeanPostProcessor() {
				@Override
				public Object postProcessBeforeInitialization(Object bean, String beanName) {
					return bean;
				}

				@Override
				public Object postProcessAfterInitialization(Object bean, String beanName) {
					if (bean instanceof RestTemplate)
						((RestTemplate) bean).getInterceptors().add(interceptor);
					return bean;
				}
			};
		}
	}
}
