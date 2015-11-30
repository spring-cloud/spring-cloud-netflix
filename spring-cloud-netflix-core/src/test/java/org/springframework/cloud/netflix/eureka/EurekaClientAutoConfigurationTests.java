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

package org.springframework.cloud.netflix.eureka;

import org.junit.After;
import org.junit.Test;
import org.springframework.aop.scope.ScopedProxyFactoryBean;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.EnvironmentTestUtils.addEnvironment;

/**
 * @author Spencer Gibb
 */
public class EurekaClientAutoConfigurationTests {
	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@After
	public void init() {
		if (this.context != null) {
			this.context.close();
		}
	}

	private void setupContext(Class<?>... config) {
		this.context.register(PropertyPlaceholderAutoConfiguration.class);
		for (Class<?> value : config) {
			this.context.register(value);
		}
		this.context.register(TestConfiguration.class);
		this.context.refresh();
	}

	@Test
	public void nonSecurePortPeriods() {
		testNonSecurePort("server.port");
	}

	@Test
	public void nonSecurePortUnderscores() {
		testNonSecurePort("SERVER_PORT");
	}

	@Test
	public void nonSecurePort() {
		testNonSecurePort("PORT");
		assertEquals("eurekaClient",
				this.context.getBeanDefinition("eurekaClient").getFactoryMethodName());
	}

	@Test
	public void managementPort() {
		EnvironmentTestUtils.addEnvironment(this.context, "server.port=8989",
				"management.port=9999");
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);
		assertTrue("Wrong status page: " + instance.getStatusPageUrl(),
				instance.getStatusPageUrl().contains("9999"));
	}

	@Test
	public void hostname() {
		EnvironmentTestUtils.addEnvironment(this.context, "server.port=8989",
				"management.port=9999", "eureka.instance.hostname=foo");
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);
		assertTrue("Wrong status page: " + instance.getStatusPageUrl(),
				instance.getStatusPageUrl().contains("foo"));
	}

	@Test
	public void refreshScopedBeans() {
		setupContext(RefreshAutoConfiguration.class);
		assertEquals(ScopedProxyFactoryBean.class.getName(),
				this.context.getBeanDefinition("eurekaClient").getBeanClassName());
		assertEquals(ScopedProxyFactoryBean.class.getName(), this.context
				.getBeanDefinition("eurekaApplicationInfoManager").getBeanClassName());
	}

	private void testNonSecurePort(String propName) {
		addEnvironment(this.context, propName + ":8888");
		setupContext();
		assertEquals(8888, getInstanceConfig().getNonSecurePort());
	}

	private EurekaInstanceConfigBean getInstanceConfig() {
		return this.context.getBean(EurekaInstanceConfigBean.class);
	}

	@Configuration
	@EnableConfigurationProperties
	@Import(EurekaClientAutoConfiguration.class)
	protected static class TestConfiguration {

	}
}
