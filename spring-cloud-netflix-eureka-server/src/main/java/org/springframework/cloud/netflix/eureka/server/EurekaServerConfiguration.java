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

package org.springframework.cloud.netflix.eureka.server;

import javax.servlet.Filter;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaServerConfigBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.google.common.collect.Lists;
import com.sun.jersey.spi.container.servlet.ServletContainer;

/**
 * @author Gunnar Hillert
 */
@Configuration
@Import(EurekaServerInitializerConfiguration.class)
@EnableDiscoveryClient
@EnableConfigurationProperties(EurekaDashboardProperties.class)
public class EurekaServerConfiguration extends WebMvcConfigurerAdapter {

	@Bean
	@ConditionalOnProperty(prefix = "eureka.dashboard", name = "enabled", matchIfMissing = true)
	public EurekaController eurekaController() {
		return new EurekaController();
	}

	@Bean
	public FilterRegistrationBean jersey() {
		FilterRegistrationBean bean = new FilterRegistrationBean();
		bean.setFilter(new ServletContainer());
		bean.setOrder(Ordered.LOWEST_PRECEDENCE);
		bean.addInitParameter("com.sun.jersey.config.property.WebPageContentRegex",
				EurekaServerConfigBean.DEFAULT_PREFIX + "/(fonts|images|css|js)/.*");
		bean.addInitParameter("com.sun.jersey.config.property.packages",
				"com.netflix.discovery;com.netflix.eureka");
		bean.setUrlPatterns(Lists.newArrayList(EurekaServerConfigBean.DEFAULT_PREFIX
				+ "/*"));
		return bean;
	}

	@Bean
	public FilterRegistrationBean traceFilterRegistration(
			@Qualifier("webRequestLoggingFilter") Filter filter) {
		FilterRegistrationBean bean = new FilterRegistrationBean();
		bean.setFilter(filter);
		bean.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
		return bean;
	}

}
