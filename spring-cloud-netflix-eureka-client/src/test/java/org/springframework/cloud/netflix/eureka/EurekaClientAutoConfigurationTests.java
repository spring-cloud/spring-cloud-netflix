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

package org.springframework.cloud.netflix.eureka;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.commons.util.UtilAutoConfiguration;
import org.springframework.cloud.context.scope.GenericScope;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClient;
import com.sun.jersey.client.apache4.ApacheHttpClient4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.springframework.boot.test.util.EnvironmentTestUtils.addEnvironment;

/**
 * @author Spencer Gibb
 * @author Matt Jenkins
 */
public class EurekaClientAutoConfigurationTests {
	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@After
	public void after() {
		if (this.context != null && this.context.isActive()) {
			this.context.close();
		}
	}

	private void setupContext(Class<?>... config) {
		ConfigurationPropertySources.attach(this.context.getEnvironment());
		this.context.register(PropertyPlaceholderAutoConfiguration.class, EurekaDiscoveryClientConfiguration.class);
		for (Class<?> value : config) {
			this.context.register(value);
		}
		this.context.register(TestConfiguration.class);
		this.context.refresh();
	}

	@Test
	public void shouldSetManagementPortInMetadataMapIfEqualToServerPort() throws Exception {
		addEnvironment(this.context, "server.port=8989");
		setupContext(RefreshAutoConfiguration.class);

		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);

		assertEquals("8989", instance.getMetadataMap().get("management.port"));
	}

	@Test
	public void shouldNotSetManagementAndJmxPortsInMetadataMap() throws Exception {
		addEnvironment(this.context, "server.port=8989", "management.server.port=0");
		setupContext(RefreshAutoConfiguration.class);

		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);

		assertEquals(null, instance.getMetadataMap().get("management.port"));
		assertEquals(null, instance.getMetadataMap().get("jmx.port"));
	}

	@Test
	public void shouldSetManagementAndJmxPortsInMetadataMap() throws Exception {
		addEnvironment(this.context, "management.server.port=9999",
				"com.sun.management.jmxremote.port=6789");
		setupContext(RefreshAutoConfiguration.class);

		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);
		assertEquals("9999", instance.getMetadataMap().get("management.port"));
		assertEquals("6789", instance.getMetadataMap().get("jmx.port"));
	}

	@Test
	public void shouldNotResetManagementAndJmxPortsInMetadataMap() throws Exception {
		addEnvironment(this.context, "management.server.port=9999",
				"eureka.instance.metadata-map.jmx.port=9898",
				"eureka.instance.metadata-map.management.port=7878");
		setupContext(RefreshAutoConfiguration.class);

		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);
		assertEquals("7878", instance.getMetadataMap().get("management.port"));
		assertEquals("9898", instance.getMetadataMap().get("jmx.port"));
	}

	@Test
	public void nonSecurePortPeriods() {
		testNonSecurePort("server.port");
	}

	@Test
	public void nonSecurePortUnderscores() {
		testNonSecurePortSystemProp("SERVER_PORT");
	}

	@Test
	public void nonSecurePort() {
		testNonSecurePortSystemProp("PORT");
		assertEquals("eurekaClient",
				this.context.getBeanDefinition("eurekaClient").getFactoryMethodName());
	}

	@Test
	public void securePortPeriods() {
		testSecurePort("server.port");
	}

	@Test
	public void securePortUnderscores() {
		TestPropertyValues.of("eureka.instance.secure-port-enabled=true").applyTo(this.context);
		addSystemEnvironment(this.context.getEnvironment(), "SERVER_PORT:8443");
		setupContext();
		assertEquals(8443, getInstanceConfig().getSecurePort());
	}

	@Test
	public void securePort() {
		testSecurePort("PORT");
		assertEquals("eurekaClient",
				this.context.getBeanDefinition("eurekaClient").getFactoryMethodName());
	}

	@Test
	public void managementPort() {
		TestPropertyValues.of("server.port=8989",
				"management.server.port=9999").applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);
		assertTrue("Wrong status page: " + instance.getStatusPageUrl(),
				instance.getStatusPageUrl().contains("9999"));
	}

	@Test
	public void statusPageUrlPathAndManagementPort() {
		TestPropertyValues.of( "server.port=8989",
				"management.server.port=9999",
				"eureka.instance.statusPageUrlPath=/myStatusPage").applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);
		assertTrue("Wrong status page: " + instance.getStatusPageUrl(),
				instance.getStatusPageUrl().contains("/myStatusPage"));
	}

	@Test
	public void healthCheckUrlPathAndManagementPort() {
		TestPropertyValues.of( "server.port=8989",
				"management.server.port=9999",
				"eureka.instance.healthCheckUrlPath=/myHealthCheck").applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);
		assertTrue("Wrong health check: " + instance.getHealthCheckUrl(),
				instance.getHealthCheckUrl().contains("/myHealthCheck"));
	}

	@Test
	public void statusPageUrl_and_healthCheckUrl_do_not_contain_server_context_path() throws Exception {
		addEnvironment(this.context, "server.port=8989",
				"management.server.port=9999", "server.contextPath=/service");

		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);
		assertTrue("Wrong status page: " + instance.getStatusPageUrl(),
				instance.getStatusPageUrl().endsWith(":9999/info"));
		assertTrue("Wrong health check: " + instance.getHealthCheckUrl(),
				instance.getHealthCheckUrl().endsWith(":9999/health"));
	}

	@Test
	public void statusPageUrl_and_healthCheckUrl_contain_management_context_path() throws Exception {
		addEnvironment(this.context,
				"server.port=8989", "management.server.context-path=/management");

		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);
		assertTrue("Wrong status page: " + instance.getStatusPageUrl(),
				instance.getStatusPageUrl().endsWith(":8989/management/info"));
		assertTrue("Wrong health check: " + instance.getHealthCheckUrl(),
				instance.getHealthCheckUrl().endsWith(":8989/management/health"));
	}

	@Test
	public void statusPageUrlPathAndManagementPortAndContextPath() {
		TestPropertyValues.of( "server.port=8989",
				"management.server.port=9999", "management.server.context-path=/manage",
				"eureka.instance.status-page-url-path=/myStatusPage").applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);
		assertTrue("Wrong status page: " + instance.getStatusPageUrl(),
				instance.getStatusPageUrl().endsWith(":9999/manage/myStatusPage"));
	}

	@Test
	public void healthCheckUrlPathAndManagementPortAndContextPath() {
		TestPropertyValues.of( "server.port=8989",
				"management.server.port=9999", "management.server.context-path=/manage",
				"eureka.instance.health-check-url-path=/myHealthCheck").applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);
		assertTrue("Wrong health check: " + instance.getHealthCheckUrl(),
				instance.getHealthCheckUrl().endsWith(":9999/manage/myHealthCheck"));
	}

	@Test
	public void statusPageUrlPathAndManagementPortAndContextPathKebobCase() {
		TestPropertyValues.of( "server.port=8989",
				"management.server.port=9999", "management.server.context-path=/manage",
				"eureka.instance.status-page-url-path=/myStatusPage").applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);
		assertTrue("Wrong status page: " + instance.getStatusPageUrl(),
				instance.getStatusPageUrl().endsWith(":9999/manage/myStatusPage"));
	}

	@Test
	public void healthCheckUrlPathAndManagementPortAndContextPathKebobCase() {
		TestPropertyValues.of( "server.port=8989",
				"management.server.port=9999", "management.server.context-path=/manage",
				"eureka.instance.health-check-url-path=/myHealthCheck").applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);
		assertTrue("Wrong health check: " + instance.getHealthCheckUrl(),
				instance.getHealthCheckUrl().endsWith(":9999/manage/myHealthCheck"));
	}

	@Test
	public void statusPageUrlPathAndManagementPortKabobCase() {
		TestPropertyValues.of( "server.port=8989",
				"management.server.port=9999",
				"eureka.instance.status-page-url-path=/myStatusPage").applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);
		assertTrue("Wrong status page: " + instance.getStatusPageUrl(),
				instance.getStatusPageUrl().contains("/myStatusPage"));
	}

	@Test
	public void statusPageUrlAndPreferIpAddress() {
		TestPropertyValues.of( "server.port=8989",
				"management.server.port=9999", "eureka.instance.hostname=foo",
				"eureka.instance.preferIpAddress:true").applyTo(this.context);

		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);

		assertEquals("statusPageUrl is wrong", "http://" + instance.getIpAddress() + ":9999/info",
				instance.getStatusPageUrl());
		assertEquals("healthCheckUrl is wrong", "http://" + instance.getIpAddress() + ":9999/health",
				instance.getHealthCheckUrl());
	}

	@Test
	public void statusPageAndHealthCheckUrlsShouldSetUserDefinedIpAddress() {
		addEnvironment(this.context, "server.port=8989",
				"management.server.port=9999", "eureka.instance.hostname=foo",
				"eureka.instance.ipAddress:192.168.13.90",
				"eureka.instance.preferIpAddress:true");

		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);

		assertEquals("statusPageUrl is wrong", "http://192.168.13.90:9999/info",
				instance.getStatusPageUrl());
		assertEquals("healthCheckUrl is wrong", "http://192.168.13.90:9999/health",
				instance.getHealthCheckUrl());
	}

	@Test
	public void healthCheckUrlPathAndManagementPortKabobCase() {
		TestPropertyValues.of( "server.port=8989",
				"management.server.port=9999",
				"eureka.instance.health-check-url-path=/myHealthCheck").applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);
		assertTrue("Wrong health check: " + instance.getHealthCheckUrl(),
				instance.getHealthCheckUrl().contains("/myHealthCheck"));
	}

	@Test
	public void statusPageUrlPathAndManagementPortUpperCase() {
		TestPropertyValues.of( "server.port=8989",
				"management.server.port=9999").applyTo(this.context);
		addSystemEnvironment(this.context.getEnvironment(), "EUREKA_INSTANCE_STATUS_PAGE_URL_PATH=/myStatusPage");
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);
		assertTrue("Wrong status page: " + instance.getStatusPageUrl(),
				instance.getStatusPageUrl().contains("/myStatusPage"));
	}

	@Test
	public void healthCheckUrlPathAndManagementPortUpperCase() {
		TestPropertyValues.of( "server.port=8989",
				"management.server.port=9999").applyTo(this.context);
		addSystemEnvironment(this.context.getEnvironment(), "EUREKA_INSTANCE_HEALTH_CHECK_URL_PATH=/myHealthCheck");
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);
		assertTrue("Wrong health check: " + instance.getHealthCheckUrl(),
				instance.getHealthCheckUrl().contains("/myHealthCheck"));
	}

	@Test
	public void hostname() {
		TestPropertyValues.of( "server.port=8989",
				"management.server.port=9999", "eureka.instance.hostname=foo").applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context
				.getBean(EurekaInstanceConfigBean.class);
		assertTrue("Wrong status page: " + instance.getStatusPageUrl(),
				instance.getStatusPageUrl().contains("foo"));
	}

	@Test
	public void refreshScopedBeans() {
		setupContext(RefreshAutoConfiguration.class);
		assertThat(this.context.getBeanDefinition("eurekaClient").getBeanClassName())
				.startsWith(GenericScope.class.getName()+"$LockedScopedProxyFactoryBean");
		assertThat(this.context.getBeanDefinition("eurekaApplicationInfoManager").getBeanClassName())
				.startsWith(GenericScope.class.getName()+"$LockedScopedProxyFactoryBean");
	}

	@Test
	public void basicAuth() {
		TestPropertyValues.of( "server.port=8989",
				"eureka.client.serviceUrl.defaultZone=http://user:foo@example.com:80/eureka").applyTo(this.context);
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
		TestPropertyValues.of( "spring.application.name=mytest").applyTo(this.context);
		setupContext();
		assertEquals("mytest", getInstanceConfig().getAppname());
		assertEquals("mytest", getInstanceConfig().getVirtualHostName());
		assertEquals("mytest", getInstanceConfig().getSecureVirtualHostName());
	}

	@Test
	public void testAppNameUpper() throws Exception {
		addSystemEnvironment(this.context.getEnvironment(), "SPRING_APPLICATION_NAME=mytestupper");
		setupContext();
		assertEquals("mytestupper", getInstanceConfig().getAppname());
		assertEquals("mytestupper", getInstanceConfig().getVirtualHostName());
		assertEquals("mytestupper", getInstanceConfig().getSecureVirtualHostName());
	}

	private void addSystemEnvironment(ConfigurableEnvironment environment, String... pairs) {
		MutablePropertySources sources = environment.getPropertySources();
		Map<String, Object> map = getOrAdd(sources, "testsysenv");
		for (String pair : pairs) {
			int index = getSeparatorIndex(pair);
			String key = pair.substring(0, index > 0 ? index : pair.length());
			String value = index > 0 ? pair.substring(index + 1) : "";
			map.put(key.trim(), value.trim());
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> getOrAdd(MutablePropertySources sources,
												String name) {
		if (sources.contains(name)) {
			return (Map<String, Object>) sources.get(name).getSource();
		}
		Map<String, Object> map = new HashMap<>();
		sources.addFirst(new SystemEnvironmentPropertySource(name, map));
		return map;
	}

	private static int getSeparatorIndex(String pair) {
		int colonIndex = pair.indexOf(":");
		int equalIndex = pair.indexOf("=");
		if (colonIndex == -1) {
			return equalIndex;
		}
		if (equalIndex == -1) {
			return colonIndex;
		}
		return Math.min(colonIndex, equalIndex);
	}

	@Test
	public void testInstanceNamePreferred() throws Exception {
		addSystemEnvironment(this.context.getEnvironment(), "SPRING_APPLICATION_NAME=mytestspringappname");
		TestPropertyValues.of( "eureka.instance.appname=mytesteurekaappname").applyTo(this.context);
		setupContext();
		assertEquals("mytesteurekaappname", getInstanceConfig().getAppname());
	}

	@Test
	public void eurekaHealthIndicatorCreated() {
		setupContext();
		this.context.getBean(EurekaHealthIndicator.class);
	}

	@Test
	public void eurekaClientClosed() {
		setupContext(TestEurekaClientConfiguration.class);
		if (this.context != null) {
			CountDownLatch latch = this.context.getBean(CountDownLatch.class);
			this.context.close();
			assertThat(latch.getCount()).isEqualTo(0);
		}
	}

	@Test
	public void eurekaRegistrationClosed() throws IOException {
		setupContext(TestEurekaRegistrationConfiguration.class);
		if (this.context != null) {
			EurekaRegistration registration = this.context.getBean(EurekaRegistration.class);
			this.context.close();
			verify(registration).close();
		}
	}

	private void testNonSecurePortSystemProp(String propName) {
		addSystemEnvironment(this.context.getEnvironment(), propName + ":8888");
		setupContext();
		assertEquals(8888, getInstanceConfig().getNonSecurePort());
	}

	private void testNonSecurePort(String propName) {
		TestPropertyValues.of(propName + ":8888").applyTo(this.context);
		setupContext();
		assertEquals(8888, getInstanceConfig().getNonSecurePort());
	}

	private void testSecurePort(String propName) {
		TestPropertyValues.of("eureka.instance.secure-port-enabled=true", propName+":8443").applyTo(this.context);
		setupContext();
		assertEquals(8443, getInstanceConfig().getSecurePort());
	}

	private EurekaInstanceConfigBean getInstanceConfig() {
		return this.context.getBean(EurekaInstanceConfigBean.class);
	}

	@Configuration
	@EnableConfigurationProperties
	@Import({ UtilAutoConfiguration.class, EurekaClientAutoConfiguration.class })
	protected static class TestConfiguration { }

	@Configuration
	protected static class TestEurekaClientConfiguration {

		@Bean
		public CountDownLatch countDownLatch() {
			return new CountDownLatch(1);
		}

		@Bean(destroyMethod = "shutdown")
		@ConditionalOnMissingBean(value = EurekaClient.class, search = SearchStrategy.CURRENT)
		public EurekaClient eurekaClient(ApplicationInfoManager manager, EurekaClientConfig config, ApplicationContext context) {
			return new CloudEurekaClient(manager, config, null, context) {
				@Override
				public synchronized void shutdown() {
					CountDownLatch latch = countDownLatch();
					if (latch.getCount() == 1) {
						latch.countDown();
					}
					super.shutdown();
				}
			};
		}
	}

	@Configuration
	protected static class TestEurekaRegistrationConfiguration {

		@Bean
		public EurekaRegistration eurekaRegistration(EurekaClient eurekaClient, CloudEurekaInstanceConfig instanceConfig, ApplicationInfoManager applicationInfoManager) {
			return spy(EurekaRegistration.builder(instanceConfig)
					.with(applicationInfoManager)
					.with(eurekaClient)
					.build());
		}
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
