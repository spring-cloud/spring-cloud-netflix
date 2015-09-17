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

package org.springframework.cloud.netflix.metrics.spectator;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.autoconfigure.MetricRepositoryAutoConfiguration;
import org.springframework.boot.actuate.endpoint.MetricReaderPublicMetrics;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.netflix.servo.publish.MetricPoller;
import com.netflix.servo.publish.MonitorRegistryMetricPoller;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.servo.ServoRegistry;

/**
 * Configures a basic Spectator registry that bridges to the legacy Servo API. We use this bridge because servo contains
 * an Atlas plugin that allows us to easily send all Spectator metrics to Atlas. Servo contains a similar plugin for
 * Graphite.
 *
 * Conditionally configures both an MVC interceptor and a RestTemplate interceptor that records metrics for request
 * handling timings.
 *
 * @author Jon Schneider
 */
@Configuration
@AutoConfigureBefore(MetricRepositoryAutoConfiguration.class)
public class SpectatorAutoConfiguration {
	@Bean
	@ConditionalOnMissingBean(Registry.class)
	Registry registry() {
		return new ServoRegistry();
	}

	@Bean
	@ConditionalOnMissingBean(MetricPoller.class)
	MetricPoller metricPoller() {
		return new MonitorRegistryMetricPoller();
	}

	// TODO why is @AutoConfigureBefore not preventing the default CounterService from registering like it does for
	// MetricsDropwizardAutoConfiguration?
	@Primary
	@Bean
	@ConditionalOnMissingBean({ SpectatorMetricServices.class })
	public SpectatorMetricServices spectatorMetricServices(Registry metricRegistry) {
		return new SpectatorMetricServices(metricRegistry);
	}

	@Bean
	public MetricReaderPublicMetrics spectatorPublicMetrics(Registry metricRegistry) {
		SpectatorMetricReader reader = new SpectatorMetricReader(metricRegistry);
		return new MetricReaderPublicMetrics(reader);
	}

	@Configuration
	@ConditionalOnWebApplication
	static class SpectatorWebResourceConfiguration extends WebMvcConfigurerAdapter {
		@Bean
		SpectatorHandlerInterceptor spectatorMonitoringWebResourceInterceptor() {
			return new SpectatorHandlerInterceptor();
		}

		@Override
		public void addInterceptors(InterceptorRegistry registry) {
			registry.addInterceptor(spectatorMonitoringWebResourceInterceptor());
		}
	}

	@Configuration
	@ConditionalOnBean({ RestTemplate.class, AopAutoConfiguration.CglibAutoProxyConfiguration.class })
	static class SpectatorRestTemplateConfiguration {
		@Bean
		RestTemplateUrlTemplateCapturingAspect restTemplateUrlTemplateCapturingAspect() {
			return new RestTemplateUrlTemplateCapturingAspect();
		}

		@Bean
		SpectatorClientHttpRequestInterceptor spectatorLoggingClientHttpRequestInterceptor() {
			return new SpectatorClientHttpRequestInterceptor();
		}

		@Bean
		BeanPostProcessor spectatorRestTemplateInterceptorPostProcessor(
				final SpectatorClientHttpRequestInterceptor interceptor) {
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
