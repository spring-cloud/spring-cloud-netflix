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
 *
 */

package org.springframework.cloud.netflix.eureka;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.aop.scope.ScopedProxyFactoryBean;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.commons.util.UtilAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClient;
import com.sun.jersey.client.apache4.ApacheHttpClient4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.util.EnvironmentTestUtils.addEnvironment;

/**
 * @author Spencer Gibb
 * @author Matt Jenkins
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
		this.context.register(PropertyPlaceholderAutoConfiguration.class, EurekaDiscoveryClientConfiguration.class);
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
	public void statusPageUrlPathAndManagementPort() {
		EnvironmentTestUtils.addEnvironment(this.context, "server.port=8989",
				"management.port=9999",
				"eureka.instance.statusPageUrlPath=/myStatusPage");
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);
		assertTrue("Wrong status page: " + instance.getStatusPageUrl(),
				instance.getStatusPageUrl().contains("/myStatusPage"));
	}

	@Test
	public void healthCheckUrlPathAndManagementPort() {
		EnvironmentTestUtils.addEnvironment(this.context, "server.port=8989",
				"management.port=9999",
				"eureka.instance.healthCheckUrlPath=/myHealthCheck");
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);
		assertTrue("Wrong health check: " + instance.getHealthCheckUrl(),
				instance.getHealthCheckUrl().contains("/myHealthCheck"));
	}

	@Test
	public void statusPageUrlPathAndManagementPortAndContextPath() {
		EnvironmentTestUtils.addEnvironment(this.context, "server.port=8989",
				"management.port=9999", "management.contextPath=/manage",
				"eureka.instance.statusPageUrlPath=/myStatusPage");
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);
		assertTrue("Wrong status page: " + instance.getStatusPageUrl(),
				instance.getStatusPageUrl().endsWith(":9999/manage/myStatusPage"));
	}

	@Test
	public void healthCheckUrlPathAndManagementPortAndContextPath() {
		EnvironmentTestUtils.addEnvironment(this.context, "server.port=8989",
				"management.port=9999", "management.contextPath=/manage",
				"eureka.instance.healthCheckUrlPath=/myHealthCheck");
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);
		assertTrue("Wrong health check: " + instance.getHealthCheckUrl(),
				instance.getHealthCheckUrl().endsWith(":9999/manage/myHealthCheck"));
	}

	@Test
	public void statusPageUrlPathAndManagementPortAndContextPathKebobCase() {
		EnvironmentTestUtils.addEnvironment(this.context, "server.port=8989",
				"management.port=9999", "management.context-path=/manage",
				"eureka.instance.statusPageUrlPath=/myStatusPage");
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);
		assertTrue("Wrong status page: " + instance.getStatusPageUrl(),
				instance.getStatusPageUrl().endsWith(":9999/manage/myStatusPage"));
	}

	@Test
	public void healthCheckUrlPathAndManagementPortAndContextPathKebobCase() {
		EnvironmentTestUtils.addEnvironment(this.context, "server.port=8989",
				"management.port=9999", "management.context-path=/manage",
				"eureka.instance.healthCheckUrlPath=/myHealthCheck");
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);
		assertTrue("Wrong health check: " + instance.getHealthCheckUrl(),
				instance.getHealthCheckUrl().endsWith(":9999/manage/myHealthCheck"));
	}

	@Test
	public void statusPageUrlPathAndManagementPortKabobCase() {
		EnvironmentTestUtils.addEnvironment(this.context, "server.port=8989",
				"management.port=9999",
				"eureka.instance.status-page-url-path=/myStatusPage");
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);
		assertTrue("Wrong status page: " + instance.getStatusPageUrl(),
				instance.getStatusPageUrl().contains("/myStatusPage"));
	}

	@Test
	public void statusPageUrlAndPreferIpAddress() {
		EnvironmentTestUtils.addEnvironment(this.context, "server.port=8989",
				"management.port=9999", "eureka.instance.hostname=foo",
				"eureka.instance.preferIpAddress:true");

		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);

		assertEquals("statusPageUrl is wrong", "http://" + instance.getIpAddress() + ":9999/info",
				instance.getStatusPageUrl());
	}

	@Test
	public void healthCheckUrlPathAndManagementPortKabobCase() {
		EnvironmentTestUtils.addEnvironment(this.context, "server.port=8989",
				"management.port=9999",
				"eureka.instance.health-check-url-path=/myHealthCheck");
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);
		assertTrue("Wrong health check: " + instance.getHealthCheckUrl(),
				instance.getHealthCheckUrl().contains("/myHealthCheck"));
	}

	@Test
	public void statusPageUrlPathAndManagementPortUpperCase() {
		EnvironmentTestUtils.addEnvironment(this.context, "server.port=8989",
				"management.port=9999",
				"EUREKA_INSTANCE_STATUS_PAGE_URL_PATH=/myStatusPage");
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);
		assertTrue("Wrong status page: " + instance.getStatusPageUrl(),
				instance.getStatusPageUrl().contains("/myStatusPage"));
	}

	@Test
	public void healthCheckUrlPathAndManagementPortUpperCase() {
		EnvironmentTestUtils.addEnvironment(this.context, "server.port=8989",
				"management.port=9999",
				"EUREKA_INSTANCE_HEALTH_CHECK_URL_PATH=/myHealthCheck");
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);
		assertTrue("Wrong health check: " + instance.getHealthCheckUrl(),
				instance.getHealthCheckUrl().contains("/myHealthCheck"));
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

	@Test
	public void basicAuth() {
		EnvironmentTestUtils.addEnvironment(this.context, "server.port=8989",
				"eureka.client.serviceUrl.defaultZone=http://user:foo@example.com:80/eureka");
		setupContext(MockClientConfiguration.class);
		// ApacheHttpClient4 http = this.context.getBean(ApacheHttpClient4.class);
		// Mockito.verify(http).addFilter(Matchers.any(HTTPBasicAuthFilter.class));
	}

	@Test
	public void testDefaultAppName() throws Exception {
		setupContext();
		assertEquals("unknown", getInstanceConfig().getAppname());
		assertEquals("unknown", getInstanceConfig().getVirtualHostName());
		assertEquals("unknown", getInstanceConfig().getSecureVirtualHostName());
	}

	@Test
	public void testAppName() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context, "spring.application.name=mytest");
		setupContext();
		assertEquals("mytest", getInstanceConfig().getAppname());
		assertEquals("mytest", getInstanceConfig().getVirtualHostName());
		assertEquals("mytest", getInstanceConfig().getSecureVirtualHostName());
	}

	@Test
	public void testAppNameUpper() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context, "SPRING_APPLICATION_NAME=mytestupper");
		setupContext();
		assertEquals("mytestupper", getInstanceConfig().getAppname());
		assertEquals("mytestupper", getInstanceConfig().getVirtualHostName());
		assertEquals("mytestupper", getInstanceConfig().getSecureVirtualHostName());
	}

	@Test
	public void testInstanceNamePreferred() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context, "SPRING_APPLICATION_NAME=mytestspringappname",
				"eureka.instance.appname=mytesteurekaappname");
		setupContext();
		assertEquals("mytesteurekaappname", getInstanceConfig().getAppname());
	}

	@Test
	public void eurekaHealthIndicatorCreated() {
		setupContext();
		this.context.getBean(EurekaHealthIndicator.class);
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
	@Import({ UtilAutoConfiguration.class, EurekaClientAutoConfiguration.class })
	protected static class TestConfiguration {

	}

	@Configuration
	protected static class MockClientConfiguration {

		@Bean
		public MutableDiscoveryClientOptionalArgs mutableDiscoveryClientOptionalArgs() {
			MutableDiscoveryClientOptionalArgs args = new MutableDiscoveryClientOptionalArgs();
			args.setEurekaJerseyClient(jerseyClient());
			return args;
		}

		@Bean
		public EurekaJerseyClient jerseyClient() {
			EurekaJerseyClient mock = Mockito.mock(EurekaJerseyClient.class);
			Mockito.when(mock.getClient()).thenReturn(apacheClient());
			return mock;
		}

		@Bean
		public ApacheHttpClient4 apacheClient() {
			return Mockito.mock(ApacheHttpClient4.class);
		}
	}
}
