/*
 * Copyright 2013-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.eureka;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.transport.jersey.TransportClientFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.health.DiscoveryClientHealthIndicator;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationProperties;
import org.springframework.cloud.commons.util.UtilAutoConfiguration;
import org.springframework.cloud.context.config.ContextRefreshedWithApplicationEvent;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.cloud.context.scope.GenericScope;
import org.springframework.cloud.netflix.eureka.config.DiscoveryClientOptionalArgsConfiguration;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaServiceRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;

/**
 * @author Spencer Gibb
 * @author Matt Jenkins
 * @author Olga Maciaszek-Sharma
 * @author Tim Ysewyn
 */
class EurekaClientAutoConfigurationTests {

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@AfterEach
	void after() {
		if (this.context != null && this.context.isActive()) {
			this.context.close();
		}
	}

	private void setupContext(Class<?>... config) {
		ConfigurationPropertySources.attach(this.context.getEnvironment());
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				DiscoveryClientOptionalArgsConfiguration.class, EurekaDiscoveryClientConfiguration.class);
		for (Class<?> value : config) {
			this.context.register(value);
		}
		this.context.register(TestConfiguration.class);
		this.context.refresh();
	}

	@Test
	void shouldSetManagementPortInMetadataMapIfEqualToServerPort() {
		TestPropertyValues.of("server.port=8989").applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class);

		EurekaInstanceConfigBean instance = this.context.getBean(EurekaInstanceConfigBean.class);

		assertThat(instance.getMetadataMap().get("management.port")).isEqualTo("8989");
	}

	@Test
	void shouldNotSetManagementAndJmxPortsInMetadataMap() {
		TestPropertyValues.of("server.port=8989", "management.server.port=0").applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class);

		EurekaInstanceConfigBean instance = this.context.getBean(EurekaInstanceConfigBean.class);

		assertThat(instance.getMetadataMap().get("management.port")).isEqualTo(null);
		assertThat(instance.getMetadataMap().get("jmx.port")).isEqualTo(null);
	}

	@Test
	void shouldSetManagementAndJmxPortsInMetadataMap() {
		TestPropertyValues.of("management.server.port=9999", "com.sun.management.jmxremote.port=6789")
				.applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class);

		EurekaInstanceConfigBean instance = this.context.getBean(EurekaInstanceConfigBean.class);
		assertThat(instance.getMetadataMap().get("management.port")).isEqualTo("9999");
		assertThat(instance.getMetadataMap().get("jmx.port")).isEqualTo("6789");
	}

	@Test
	void shouldNotResetManagementAndJmxPortsInMetadataMap() {
		TestPropertyValues.of("management.server.port=9999", "eureka.instance.metadata-map.jmx.port=9898",
				"eureka.instance.metadata-map.management.port=7878").applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class);

		EurekaInstanceConfigBean instance = this.context.getBean(EurekaInstanceConfigBean.class);
		assertThat(instance.getMetadataMap().get("management.port")).isEqualTo("7878");
		assertThat(instance.getMetadataMap().get("jmx.port")).isEqualTo("9898");
	}

	@Test
	void nonSecurePortPeriods() {
		testNonSecurePort("server.port");
	}

	@Test
	void nonSecurePortUnderscores() {
		testNonSecurePortSystemProp("SERVER_PORT");
	}

	@Test
	void nonSecurePort() {
		testNonSecurePortSystemProp("PORT");
		assertThat(this.context.getBeanDefinition("eurekaClient").getFactoryMethodName()).isEqualTo("eurekaClient");
	}

	@Test
	void securePortPeriods() {
		testSecurePort("server.port");
	}

	@Test
	void securePortUnderscores() {
		TestPropertyValues.of("eureka.instance.secure-port-enabled=true").applyTo(this.context);
		addSystemEnvironment(this.context.getEnvironment(), "SERVER_PORT:8443");
		setupContext();
		assertThat(getInstanceConfig().getSecurePort()).isEqualTo(8443);
	}

	@Test
	void securePort() {
		testSecurePort("PORT");
		assertThat(this.context.getBeanDefinition("eurekaClient").getFactoryMethodName()).isEqualTo("eurekaClient");
	}

	@Test
	void managementPort() {
		TestPropertyValues.of("server.port=8989", "management.server.port=9999").applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context.getBean(EurekaInstanceConfigBean.class);
		assertThat(instance.getStatusPageUrl().contains("9999")).as("Wrong status page: " + instance.getStatusPageUrl())
				.isTrue();
	}

	@Test
	void statusPageUrlPathAndManagementPort() {
		TestPropertyValues.of("server.port=8989", "management.server.port=9999",
				"eureka.instance.statusPageUrlPath=/myStatusPage").applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context.getBean(EurekaInstanceConfigBean.class);
		assertThat(instance.getStatusPageUrl().contains("/myStatusPage"))
				.as("Wrong status page: " + instance.getStatusPageUrl()).isTrue();
	}

	@Test
	void healthCheckUrlPathAndManagementPort() {
		TestPropertyValues.of("server.port=8989", "management.server.port=9999",
				"eureka.instance.healthCheckUrlPath=/myHealthCheck").applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context.getBean(EurekaInstanceConfigBean.class);
		assertThat(instance.getHealthCheckUrl().contains("/myHealthCheck"))
				.as("Wrong health check: " + instance.getHealthCheckUrl()).isTrue();
	}

	@Test
	void statusPageUrl_and_healthCheckUrl_do_not_contain_server_context_path() {
		TestPropertyValues.of("server.port=8989", "management.server.port=9999", "server.contextPath=/service")
				.applyTo(this.context);

		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context.getBean(EurekaInstanceConfigBean.class);
		assertThat(instance.getStatusPageUrl().endsWith(":9999/actuator/info"))
				.as("Wrong status page: " + instance.getStatusPageUrl()).isTrue();
		assertThat(instance.getHealthCheckUrl().endsWith(":9999/actuator/health"))
				.as("Wrong health check: " + instance.getHealthCheckUrl()).isTrue();
	}

	@Test
	void statusPageUrl_and_healthCheckUrl_contain_management_context_path() {
		TestPropertyValues.of("server.port=8989", "management.server.servlet.context-path=/management")
				.applyTo(this.context);

		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context.getBean(EurekaInstanceConfigBean.class);
		assertThat(instance.getStatusPageUrl().endsWith(":8989/management/actuator/info"))
				.as("Wrong status page: " + instance.getStatusPageUrl()).isTrue();
		assertThat(instance.getHealthCheckUrl().endsWith(":8989/management/actuator/health"))
				.as("Wrong health check: " + instance.getHealthCheckUrl()).isTrue();
	}

	@Test
	void statusPageUrl_and_healthCheckUrl_contain_management_context_path_random_port() {
		TestPropertyValues.of("server.port=0", "management.server.servlet.context-path=/management")
				.applyTo(this.context);

		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context.getBean(EurekaInstanceConfigBean.class);
		assertThat(instance.getStatusPageUrlPath().equals("/management/actuator/info"))
				.as("Wrong status page: " + instance.getStatusPageUrlPath()).isTrue();
		assertThat(instance.getHealthCheckUrlPath().equals("/management/actuator/health"))
				.as("Wrong health check: " + instance.getHealthCheckUrlPath()).isTrue();
	}

	@Test
	void statusPageUrlPathAndManagementPortAndContextPath() {
		TestPropertyValues.of("server.port=8989", "management.server.port=9999",
				"management.server.servlet.context-path=/manage", "eureka.instance.status-page-url-path=/myStatusPage")
				.applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context.getBean(EurekaInstanceConfigBean.class);
		assertThat(instance.getStatusPageUrl().endsWith(":9999/manage/myStatusPage"))
				.as("Wrong status page: " + instance.getStatusPageUrl()).isTrue();
	}

	@Test
	void healthCheckUrlPathAndManagementPortAndContextPath() {
		TestPropertyValues
				.of("server.port=8989", "management.server.port=9999", "management.server.servlet.context-path=/manage",
						"eureka.instance.health-check-url-path=/myHealthCheck")
				.applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context.getBean(EurekaInstanceConfigBean.class);
		assertThat(instance.getHealthCheckUrl().endsWith(":9999/manage/myHealthCheck"))
				.as("Wrong health check: " + instance.getHealthCheckUrl()).isTrue();
	}

	@Test
	void statusPageUrlPathAndManagementPortAndContextPathKebobCase() {
		TestPropertyValues.of("server.port=8989", "management.server.port=9999",
				"management.server.servlet.context-path=/manage", "eureka.instance.status-page-url-path=/myStatusPage")
				.applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context.getBean(EurekaInstanceConfigBean.class);
		assertThat(instance.getStatusPageUrl().endsWith(":9999/manage/myStatusPage"))
				.as("Wrong status page: " + instance.getStatusPageUrl()).isTrue();
	}

	@Test
	void healthCheckUrlPathAndManagementPortAndContextPathKebobCase() {
		TestPropertyValues
				.of("server.port=8989", "management.server.port=9999", "management.server.servlet.context-path=/manage",
						"eureka.instance.health-check-url-path=/myHealthCheck")
				.applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context.getBean(EurekaInstanceConfigBean.class);
		assertThat(instance.getHealthCheckUrl().endsWith(":9999/manage/myHealthCheck"))
				.as("Wrong health check: " + instance.getHealthCheckUrl()).isTrue();
	}

	@Test
	void statusPageUrl_and_healthCheckUrl_contain_management_base_path() {
		TestPropertyValues.of("server.port=8989", "management.server.base-path=/management").applyTo(this.context);

		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context.getBean(EurekaInstanceConfigBean.class);
		assertThat(instance.getStatusPageUrl().endsWith(":8989/management/actuator/info"))
				.as("Wrong status page: " + instance.getStatusPageUrl()).isTrue();
		assertThat(instance.getHealthCheckUrl().endsWith(":8989/management/actuator/health"))
				.as("Wrong health check: " + instance.getHealthCheckUrl()).isTrue();
	}

	@Test
	void statusPageUrl_and_healthCheckUrl_contain_management_base_path_random_port() {
		TestPropertyValues.of("server.port=0", "management.server.base-path=/management").applyTo(this.context);

		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context.getBean(EurekaInstanceConfigBean.class);
		assertThat(instance.getStatusPageUrlPath().equals("/management/actuator/info"))
				.as("Wrong status page: " + instance.getStatusPageUrlPath()).isTrue();
		assertThat(instance.getHealthCheckUrlPath().equals("/management/actuator/health"))
				.as("Wrong health check: " + instance.getHealthCheckUrlPath()).isTrue();
	}

	@Test
	void statusPageUrlPathAndManagementPortAndBasePath() {
		TestPropertyValues.of("server.port=8989", "management.server.port=9999", "management.server.base-path=/manage",
				"eureka.instance.status-page-url-path=/myStatusPage").applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context.getBean(EurekaInstanceConfigBean.class);
		assertThat(instance.getStatusPageUrl().endsWith(":9999/manage/myStatusPage"))
				.as("Wrong status page: " + instance.getStatusPageUrl()).isTrue();
	}

	@Test
	void healthCheckUrlPathAndManagementPortAndBasePath() {
		TestPropertyValues.of("server.port=8989", "management.server.port=9999", "management.server.base-path=/manage",
				"eureka.instance.health-check-url-path=/myHealthCheck").applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context.getBean(EurekaInstanceConfigBean.class);
		assertThat(instance.getHealthCheckUrl().endsWith(":9999/manage/myHealthCheck"))
				.as("Wrong health check: " + instance.getHealthCheckUrl()).isTrue();
	}

	@Test
	void statusPageUrlPathAndManagementPortAndBasePathKebobCase() {
		TestPropertyValues.of("server.port=8989", "management.server.port=9999", "management.server.base-path=/manage",
				"eureka.instance.status-page-url-path=/myStatusPage").applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context.getBean(EurekaInstanceConfigBean.class);
		assertThat(instance.getStatusPageUrl().endsWith(":9999/manage/myStatusPage"))
				.as("Wrong status page: " + instance.getStatusPageUrl()).isTrue();
	}

	@Test
	void healthCheckUrlPathAndManagementPortAndBasePathKebobCase() {
		TestPropertyValues.of("server.port=8989", "management.server.port=9999", "management.server.base-path=/manage",
				"eureka.instance.health-check-url-path=/myHealthCheck").applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context.getBean(EurekaInstanceConfigBean.class);
		assertThat(instance.getHealthCheckUrl().endsWith(":9999/manage/myHealthCheck"))
				.as("Wrong health check: " + instance.getHealthCheckUrl()).isTrue();
	}

	@Test
	void healthCheckUrlPathWithServerPortAndContextPathKebobCase() {
		TestPropertyValues
				.of("server.port=8989", "server.servlet.context-path=/servletContextPath",
						"eureka.instance.health-check-url-path=${server.servlet.context-path:}/myHealthCheck")
				.applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context.getBean(EurekaInstanceConfigBean.class);
		assertThat(instance.getHealthCheckUrl()).as("Wrong health check: " + instance.getHealthCheckUrl())
				.endsWith(":8989/servletContextPath/myHealthCheck");
	}

	@Test
	void statusPageUrlPathAndManagementPortKabobCase() {
		TestPropertyValues.of("server.port=8989", "management.server.port=9999",
				"eureka.instance.status-page-url-path=/myStatusPage").applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context.getBean(EurekaInstanceConfigBean.class);
		assertThat(instance.getStatusPageUrl().contains("/myStatusPage"))
				.as("Wrong status page: " + instance.getStatusPageUrl()).isTrue();
	}

	@Test
	void statusPageUrlAndPreferIpAddress() {
		TestPropertyValues.of("server.port=8989", "management.server.port=9999", "eureka.instance.hostname=foo",
				"eureka.instance.prefer-ip-address:true").applyTo(this.context);

		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context.getBean(EurekaInstanceConfigBean.class);

		assertThat(instance.getStatusPageUrl()).as("statusPageUrl is wrong")
				.isEqualTo("http://" + instance.getIpAddress() + ":9999/actuator/info");
		assertThat(instance.getHealthCheckUrl()).as("healthCheckUrl is wrong")
				.isEqualTo("http://" + instance.getIpAddress() + ":9999/actuator/health");
	}

	@Test
	void statusPageAndHealthCheckUrlsShouldSetUserDefinedIpAddress() {
		TestPropertyValues
				.of("server.port=8989", "management.server.port=9999", "eureka.instance.hostname=foo",
						"eureka.instance.ip-address:192.168.13.90", "eureka.instance.prefer-ip-address:true")
				.applyTo(this.context);

		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context.getBean(EurekaInstanceConfigBean.class);

		assertThat(instance.getStatusPageUrl()).as("statusPageUrl is wrong")
				.isEqualTo("http://192.168.13.90:9999/actuator/info");
		assertThat(instance.getHealthCheckUrl()).as("healthCheckUrl is wrong")
				.isEqualTo("http://192.168.13.90:9999/actuator/health");
	}

	@Test
	void healthCheckUrlPathAndManagementPortKabobCase() {
		TestPropertyValues.of("server.port=8989", "management.server.port=9999",
				"eureka.instance.health-check-url-path=/myHealthCheck").applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context.getBean(EurekaInstanceConfigBean.class);
		assertThat(instance.getHealthCheckUrl().contains("/myHealthCheck"))
				.as("Wrong health check: " + instance.getHealthCheckUrl()).isTrue();
	}

	@Test
	void statusPageUrlPathAndManagementPortUpperCase() {
		TestPropertyValues.of("server.port=8989", "management.server.port=9999").applyTo(this.context);
		addSystemEnvironment(this.context.getEnvironment(), "EUREKA_INSTANCE_STATUS_PAGE_URL_PATH=/myStatusPage");
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context.getBean(EurekaInstanceConfigBean.class);
		assertThat(instance.getStatusPageUrl().contains("/myStatusPage"))
				.as("Wrong status page: " + instance.getStatusPageUrl()).isTrue();
	}

	@Test
	void healthCheckUrlPathAndManagementPortUpperCase() {
		TestPropertyValues.of("server.port=8989", "management.server.port=9999").applyTo(this.context);
		addSystemEnvironment(this.context.getEnvironment(), "EUREKA_INSTANCE_HEALTH_CHECK_URL_PATH=/myHealthCheck");
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context.getBean(EurekaInstanceConfigBean.class);
		assertThat(instance.getHealthCheckUrl().contains("/myHealthCheck"))
				.as("Wrong health check: " + instance.getHealthCheckUrl()).isTrue();
	}

	@Test
	void hostname() {
		TestPropertyValues.of("server.port=8989", "management.server.port=9999", "eureka.instance.hostname=foo")
				.applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class);
		EurekaInstanceConfigBean instance = this.context.getBean(EurekaInstanceConfigBean.class);
		assertThat(instance.getStatusPageUrl().contains("foo")).as("Wrong status page: " + instance.getStatusPageUrl())
				.isTrue();
	}

	@Test
	void refreshScopedBeans() {
		setupContext(RefreshAutoConfiguration.class);
		assertThat(this.context.getBeanDefinition("eurekaClient").getBeanClassName())
				.startsWith(GenericScope.class.getName() + "$LockedScopedProxyFactoryBean");
		assertThat(this.context.getBeanDefinition("eurekaApplicationInfoManager").getBeanClassName())
				.startsWith(GenericScope.class.getName() + "$LockedScopedProxyFactoryBean");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	void shouldReregisterHealthCheckHandlerAfterRefresh() throws Exception {
		TestPropertyValues
				.of("eureka.client.healthcheck.enabled=true", "spring.cloud.config.import-check.enabled=false",
						"spring.cloud.refresh.additionalPropertySourcesToRetain=test")
				.applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class, AutoServiceRegistrationConfiguration.class);

		EurekaClient oldEurekaClient = getLazyInitEurekaClient();

		HealthCheckHandler healthCheckHandler = this.context.getBean("eurekaHealthCheckHandler",
				HealthCheckHandler.class);

		assertThat(healthCheckHandler).isInstanceOf(EurekaHealthCheckHandler.class);
		assertThat(oldEurekaClient.getHealthCheckHandler()).isSameAs(healthCheckHandler);

		ContextRefresher refresher = this.context.getBean(ContextRefresher.class);
		if (refresher instanceof ApplicationListener) {
			ApplicationListener<ContextRefreshedWithApplicationEvent> listener = (ApplicationListener) refresher;
			listener.onApplicationEvent(new ContextRefreshedWithApplicationEvent(Mockito.mock(SpringApplication.class),
					new String[0], this.context));
		}
		refresher.refresh();

		EurekaClient newEurekaClient = getLazyInitEurekaClient();
		HealthCheckHandler newHealthCheckHandler = this.context.getBean("eurekaHealthCheckHandler",
				HealthCheckHandler.class);

		assertThat(healthCheckHandler).isSameAs(newHealthCheckHandler);
		assertThat(oldEurekaClient).isNotSameAs(newEurekaClient);
		assertThat(newEurekaClient.getHealthCheckHandler()).isSameAs(healthCheckHandler);
	}

	@Test
	void shouldCloseDiscoveryClient() throws Exception {
		TestPropertyValues.of("eureka.client.healthcheck.enabled=true").applyTo(this.context);
		setupContext(RefreshAutoConfiguration.class, AutoServiceRegistrationConfiguration.class);

		AtomicBoolean isShutdown = (AtomicBoolean) ReflectionTestUtils.getField(getLazyInitEurekaClient(),
				"isShutdown");

		assertThat(isShutdown.get()).isFalse();

		this.context.close();

		assertThat(isShutdown.get()).isTrue();
	}

	@Test
	void testDefaultAppName() {
		setupContext();
		assertThat(getInstanceConfig().getAppname()).isEqualTo("unknown");
		assertThat(getInstanceConfig().getVirtualHostName()).isEqualTo("unknown");
		assertThat(getInstanceConfig().getSecureVirtualHostName()).isEqualTo("unknown");
	}

	@Test
	void testAppName() {
		TestPropertyValues.of("spring.application.name=mytest").applyTo(this.context);
		setupContext();
		assertThat(getInstanceConfig().getAppname()).isEqualTo("mytest");
		assertThat(getInstanceConfig().getVirtualHostName()).isEqualTo("mytest");
		assertThat(getInstanceConfig().getSecureVirtualHostName()).isEqualTo("mytest");
	}

	@Test
	void testAppNameUpper() {
		addSystemEnvironment(this.context.getEnvironment(), "SPRING_APPLICATION_NAME=mytestupper");
		setupContext();
		assertThat(getInstanceConfig().getAppname()).isEqualTo("mytestupper");
		assertThat(getInstanceConfig().getVirtualHostName()).isEqualTo("mytestupper");
		assertThat(getInstanceConfig().getSecureVirtualHostName()).isEqualTo("mytestupper");
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
	private static Map<String, Object> getOrAdd(MutablePropertySources sources, String name) {
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
	void testInstanceNamePreferred() {
		addSystemEnvironment(this.context.getEnvironment(), "SPRING_APPLICATION_NAME=mytestspringappname");
		TestPropertyValues.of("eureka.instance.appname=mytesteurekaappname").applyTo(this.context);
		setupContext();
		assertThat(getInstanceConfig().getAppname()).isEqualTo("mytesteurekaappname");
	}

	@Test
	void eurekaHealthIndicatorCreated() {
		setupContext();
		this.context.getBean(EurekaHealthIndicator.class);
	}

	@Test
	void eurekaClientClosed() {
		setupContext(TestEurekaClientConfiguration.class);
		if (this.context != null) {
			CountDownLatch latch = this.context.getBean(CountDownLatch.class);
			this.context.close();
			assertThat(latch.getCount()).isEqualTo(0);
		}
	}

	@Test
	void eurekaConfigNotLoadedWhenDiscoveryClientDisabled() {
		TestPropertyValues.of("spring.cloud.discovery.enabled=false").applyTo(this.context);
		setupContext(TestConfiguration.class);
		assertBeanNotPresent(EurekaClientConfigBean.class);
		assertBeanNotPresent(EurekaInstanceConfigBean.class);
		assertBeanNotPresent(DiscoveryClient.class);
		assertBeanNotPresent(EurekaServiceRegistry.class);
		assertBeanNotPresent(EurekaClient.class);
	}

	@Test
	void shouldNotHaveDiscoveryClientWhenBlockingDiscoveryDisabled() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(UtilAutoConfiguration.class,
						DiscoveryClientOptionalArgsConfiguration.class, EurekaClientAutoConfiguration.class,
						EurekaDiscoveryClientConfiguration.class))
				.withPropertyValues("spring.cloud.discovery.blocking.enabled=false").run(context -> {
					assertThat(context).doesNotHaveBean(DiscoveryClient.class);
					assertThat(context).doesNotHaveBean(DiscoveryClientHealthIndicator.class);
				});
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void assertBeanNotPresent(Class beanClass) {
		try {
			context.getBean(beanClass);
			fail("Bean of type " + beanClass + " should not have been created.");
		}
		catch (NoSuchBeanDefinitionException exception) {
			// expected exception
		}
	}

	private void testNonSecurePortSystemProp(String propName) {
		addSystemEnvironment(this.context.getEnvironment(), propName + ":8888");
		setupContext();
		assertThat(getInstanceConfig().getNonSecurePort()).isEqualTo(8888);
	}

	private void testNonSecurePort(String propName) {
		TestPropertyValues.of(propName + ":8888").applyTo(this.context);
		setupContext();
		assertThat(getInstanceConfig().getNonSecurePort()).isEqualTo(8888);
	}

	private void testSecurePort(String propName) {
		TestPropertyValues.of("eureka.instance.secure-port-enabled=true", propName + ":8443").applyTo(this.context);
		setupContext();
		assertThat(getInstanceConfig().getSecurePort()).isEqualTo(8443);
	}

	private EurekaInstanceConfigBean getInstanceConfig() {
		return this.context.getBean(EurekaInstanceConfigBean.class);
	}

	private EurekaClient getLazyInitEurekaClient() throws Exception {
		return (EurekaClient) ((Advised) this.context.getBean("eurekaClient", EurekaClient.class)).getTargetSource()
				.getTarget();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	@Import({ UtilAutoConfiguration.class, EurekaClientAutoConfiguration.class })
	protected static class TestConfiguration {

	}

	@Configuration
	protected static class TestEurekaClientConfiguration {

		@Bean
		public CountDownLatch countDownLatch() {
			return new CountDownLatch(1);
		}

		@Bean(destroyMethod = "shutdown")
		@ConditionalOnMissingBean(value = EurekaClient.class, search = SearchStrategy.CURRENT)
		public EurekaClient eurekaClient(ApplicationInfoManager manager, EurekaClientConfig config,
				TransportClientFactories<?> transportClientFactories, ApplicationContext context,
				AbstractDiscoveryClientOptionalArgs optionalArgs) {
			return new CloudEurekaClient(manager, config, transportClientFactories, optionalArgs, context) {
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

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(AutoServiceRegistrationProperties.class)
	public static class AutoServiceRegistrationConfiguration {

	}

}
