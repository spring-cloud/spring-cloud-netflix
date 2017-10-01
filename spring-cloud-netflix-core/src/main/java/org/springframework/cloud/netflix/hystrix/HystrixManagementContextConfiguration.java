/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.hystrix;

import com.netflix.hystrix.contrib.javanica.aop.aspectj.HystrixCommandAspect;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ManagementServletContext;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.context.annotation.Bean;

import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsStreamServlet;

/**
 * See original {@link org.springframework.boot.actuate.autoconfigure.jolokia.JolokiaManagementContextConfiguration}
 */
@ManagementContextConfiguration
@ConditionalOnProperty(value = "management.hystrix.enabled", matchIfMissing = true)
@ConditionalOnWebApplication
@ConditionalOnBean(HystrixCommandAspect.class) // only install the stream if enabled
@ConditionalOnClass({ Health.class, HystrixMetricsStreamServlet.class })
@EnableConfigurationProperties(HystrixProperties.class)
class HystrixManagementContextConfiguration {

	private final ManagementServletContext managementServletContext;

	private final HystrixProperties properties;

	public HystrixManagementContextConfiguration(
			ManagementServletContext managementServletContext,
			HystrixProperties properties) {
		this.managementServletContext = managementServletContext;
		this.properties = properties;
	}

	@Bean
	public ServletRegistrationBean<HystrixMetricsStreamServlet> hystrixMetricsStreamServlet() {
		String path = this.managementServletContext.getContextPath()
				+ this.properties.getPath();
		String urlMapping = (path.endsWith("/") ? path + "*" : path + "/*");
		ServletRegistrationBean<HystrixMetricsStreamServlet> registration = new ServletRegistrationBean<>(
				new HystrixMetricsStreamServlet(), urlMapping);
		registration.setInitParameters(this.properties.getConfig());
		return registration;
	}

	@Bean
	public HasFeatures hystrixStreamFeature() {
		return HasFeatures.namedFeature("Hystrix Stream Servlet", HystrixMetricsStreamServlet.class);
	}
}
