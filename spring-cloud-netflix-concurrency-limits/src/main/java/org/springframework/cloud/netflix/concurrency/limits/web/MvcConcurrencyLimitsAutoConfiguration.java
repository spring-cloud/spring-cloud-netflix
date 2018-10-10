/*
 * Copyright 2013-2018 the original author or authors.
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
 *
 */
package org.springframework.cloud.netflix.concurrency.limits.web;

import java.util.function.Consumer;

import javax.servlet.http.HttpServletRequest;

import com.netflix.concurrency.limits.Limiter;
import com.netflix.concurrency.limits.servlet.ServletLimiterBuilder;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass({HttpServletRequest.class, HandlerInterceptor.class})
public class MvcConcurrencyLimitsAutoConfiguration implements WebMvcConfigurer {

	private final ObjectProvider<Consumer<ServletLimiterBuilder>> configurerProvider;

	public MvcConcurrencyLimitsAutoConfiguration(ObjectProvider<Consumer<ServletLimiterBuilder>> configurerProvider) {
		this.configurerProvider = configurerProvider;
	}

	@Bean
	@ConditionalOnMissingBean
	public Limiter<HttpServletRequest> servletLimiter() {
		ServletLimiterBuilder builder = new ServletLimiterBuilder();

		this.configurerProvider.ifAvailable(consumer -> consumer.accept(builder));

		return builder.build();
	}

	@Configuration
	protected static class HandlerInterceptorConfiguration implements WebMvcConfigurer {

		@Autowired
		private Limiter<HttpServletRequest> limiter;


		@Override
		public void addInterceptors(InterceptorRegistry registry) {
			registry.addInterceptor(new ConcurrencyLimitsHandlerInterceptor(limiter));
		}
	}
}