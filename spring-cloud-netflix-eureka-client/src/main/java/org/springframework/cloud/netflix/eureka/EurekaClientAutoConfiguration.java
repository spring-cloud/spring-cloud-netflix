/*
 * Copyright 2013-2019 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.client.CommonsClientAutoConfiguration;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.client.discovery.noop.NoopDiscoveryClientAutoConfiguration;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationProperties;
import org.springframework.cloud.client.serviceregistry.ServiceRegistryAutoConfiguration;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.cloud.netflix.eureka.config.DiscoveryClientOptionalArgsConfiguration;
import org.springframework.cloud.netflix.eureka.metadata.DefaultManagementMetadataProvider;
import org.springframework.cloud.netflix.eureka.metadata.ManagementMetadata;
import org.springframework.cloud.netflix.eureka.metadata.ManagementMetadataProvider;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaAutoServiceRegistration;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaServiceRegistry;
import org.springframework.cloud.util.ProxyUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

import static org.springframework.cloud.commons.util.IdUtils.getDefaultInstanceId;

/**
 * @author Dave Syer
 * @author Spencer Gibb
 * @author Jon Schneider
 * @author Matt Jenkins
 * @author Ryan Baxter
 * @author Daniel Lavoie
 * @author Olga Maciaszek-Sharma
 * @author Tim Ysewyn
 */
@Configuration
@EnableConfigurationProperties
@ConditionalOnClass(EurekaClientConfig.class)
@Import(DiscoveryClientOptionalArgsConfiguration.class)
@ConditionalOnProperty(value = "eureka.client.enabled", matchIfMissing = true)
@ConditionalOnDiscoveryEnabled
@AutoConfigureBefore({ NoopDiscoveryClientAutoConfiguration.class,
		CommonsClientAutoConfiguration.class, ServiceRegistryAutoConfiguration.class })
@AutoConfigureAfter(name = {
		"org.springframework.cloud.autoconfigure.RefreshAutoConfiguration",
		"org.springframework.cloud.netflix.eureka.EurekaDiscoveryClientConfiguration",
		"org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationAutoConfiguration" })
public class EurekaClientAutoConfiguration {

	private ConfigurableEnvironment env;

	public EurekaClientAutoConfiguration(ConfigurableEnvironment env) {
		this.env = env;
	}

	@Bean
	public HasFeatures eurekaFeature() {
		return HasFeatures.namedFeature("Eureka Client", EurekaClient.class);
	}

	@Bean
	@ConditionalOnMissingBean(value = EurekaClientConfig.class,
			search = SearchStrategy.CURRENT)
	public EurekaClientConfigBean eurekaClientConfigBean(ConfigurableEnvironment env) {
		EurekaClientConfigBean client = new EurekaClientConfigBean();
		if ("bootstrap".equals(this.env.getProperty("spring.config.name"))) {
			// We don't register during bootstrap by default, but there will be another
			// chance later.
			client.setRegisterWithEureka(false);
		}
		return client;
	}

	@Bean
	@ConditionalOnMissingBean
	public ManagementMetadataProvider serviceManagementMetadataProvider() {
		return new DefaultManagementMetadataProvider();
	}

	private String getProperty(String property) {
		return this.env.containsProperty(property) ? this.env.getProperty(property) : "";
	}

	@Bean
	@ConditionalOnMissingBean(value = EurekaInstanceConfig.class,
			search = SearchStrategy.CURRENT)
	public EurekaInstanceConfigBean eurekaInstanceConfigBean(InetUtils inetUtils,
			ManagementMetadataProvider managementMetadataProvider) {
		String hostname = getProperty("eureka.instance.hostname");
		boolean preferIpAddress = Boolean
				.parseBoolean(getProperty("eureka.instance.prefer-ip-address"));
		String ipAddress = getProperty("eureka.instance.ip-address");
		boolean isSecurePortEnabled = Boolean
				.parseBoolean(getProperty("eureka.instance.secure-port-enabled"));

		String serverContextPath = env.getProperty("server.servlet.context-path", "/");
		int serverPort = Integer.parseInt(
				env.getProperty("server.port", env.getProperty("port", "8080")));

		Integer managementPort = env.getProperty("management.server.port", Integer.class);
		String managementContextPath = env
				.getProperty("management.server.servlet.context-path");
		Integer jmxPort = env.getProperty("com.sun.management.jmxremote.port",
				Integer.class);
		EurekaInstanceConfigBean instance = new EurekaInstanceConfigBean(inetUtils);

		instance.setNonSecurePort(serverPort);
		instance.setInstanceId(getDefaultInstanceId(env));
		instance.setPreferIpAddress(preferIpAddress);
		instance.setSecurePortEnabled(isSecurePortEnabled);
		if (StringUtils.hasText(ipAddress)) {
			instance.setIpAddress(ipAddress);
		}

		if (isSecurePortEnabled) {
			instance.setSecurePort(serverPort);
		}

		if (StringUtils.hasText(hostname)) {
			instance.setHostname(hostname);
		}
		String statusPageUrlPath = getProperty("eureka.instance.status-page-url-path");
		String healthCheckUrlPath = getProperty("eureka.instance.health-check-url-path");

		if (StringUtils.hasText(statusPageUrlPath)) {
			instance.setStatusPageUrlPath(statusPageUrlPath);
		}
		if (StringUtils.hasText(healthCheckUrlPath)) {
			instance.setHealthCheckUrlPath(healthCheckUrlPath);
		}

		ManagementMetadata metadata = managementMetadataProvider.get(instance, serverPort,
				serverContextPath, managementContextPath, managementPort);

		if (metadata != null) {
			instance.setStatusPageUrl(metadata.getStatusPageUrl());
			instance.setHealthCheckUrl(metadata.getHealthCheckUrl());
			if (instance.isSecurePortEnabled()) {
				instance.setSecureHealthCheckUrl(metadata.getSecureHealthCheckUrl());
			}
			Map<String, String> metadataMap = instance.getMetadataMap();
			metadataMap.computeIfAbsent("management.port",
					k -> String.valueOf(metadata.getManagementPort()));
		}
		else {
			// without the metadata the status and health check URLs will not be set
			// and the status page and health check url paths will not include the
			// context path so set them here
			if (StringUtils.hasText(managementContextPath)) {
				instance.setHealthCheckUrlPath(
						managementContextPath + instance.getHealthCheckUrlPath());
				instance.setStatusPageUrlPath(
						managementContextPath + instance.getStatusPageUrlPath());
			}
		}

		setupJmxPort(instance, jmxPort);
		return instance;
	}

	private void setupJmxPort(EurekaInstanceConfigBean instance, Integer jmxPort) {
		Map<String, String> metadataMap = instance.getMetadataMap();
		if (metadataMap.get("jmx.port") == null && jmxPort != null) {
			metadataMap.put("jmx.port", String.valueOf(jmxPort));
		}
	}

	@Bean
	public EurekaServiceRegistry eurekaServiceRegistry() {
		return new EurekaServiceRegistry();
	}

	// @Bean
	// @ConditionalOnBean(AutoServiceRegistrationProperties.class)
	// @ConditionalOnProperty(value =
	// "spring.cloud.service-registry.auto-registration.enabled", matchIfMissing = true)
	// public EurekaRegistration eurekaRegistration(EurekaClient eurekaClient,
	// CloudEurekaInstanceConfig instanceConfig, ApplicationInfoManager
	// applicationInfoManager, ObjectProvider<HealthCheckHandler> healthCheckHandler) {
	// return EurekaRegistration.builder(instanceConfig)
	// .with(applicationInfoManager)
	// .with(eurekaClient)
	// .with(healthCheckHandler)
	// .build();
	// }

	@Bean
	@ConditionalOnBean(AutoServiceRegistrationProperties.class)
	@ConditionalOnProperty(
			value = "spring.cloud.service-registry.auto-registration.enabled",
			matchIfMissing = true)
	public EurekaAutoServiceRegistration eurekaAutoServiceRegistration(
			ApplicationContext context, EurekaServiceRegistry registry,
			EurekaRegistration registration) {
		return new EurekaAutoServiceRegistration(context, registry, registration);
	}

	@Configuration
	@ConditionalOnMissingRefreshScope
	protected static class EurekaClientConfiguration {

		@Autowired
		private ApplicationContext context;

		@Autowired
		private AbstractDiscoveryClientOptionalArgs<?> optionalArgs;

		@Bean(destroyMethod = "shutdown")
		@ConditionalOnMissingBean(value = EurekaClient.class,
				search = SearchStrategy.CURRENT)
		public EurekaClient eurekaClient(ApplicationInfoManager manager,
				EurekaClientConfig config) {
			return new CloudEurekaClient(manager, config, this.optionalArgs,
					this.context);
		}

		@Bean
		@ConditionalOnMissingBean(value = ApplicationInfoManager.class,
				search = SearchStrategy.CURRENT)
		public ApplicationInfoManager eurekaApplicationInfoManager(
				EurekaInstanceConfig config) {
			InstanceInfo instanceInfo = new InstanceInfoFactory().create(config);
			return new ApplicationInfoManager(config, instanceInfo);
		}

		@Bean
		@ConditionalOnBean(AutoServiceRegistrationProperties.class)
		@ConditionalOnProperty(
				value = "spring.cloud.service-registry.auto-registration.enabled",
				matchIfMissing = true)
		public EurekaRegistration eurekaRegistration(EurekaClient eurekaClient,
				CloudEurekaInstanceConfig instanceConfig,
				ApplicationInfoManager applicationInfoManager, @Autowired(
						required = false) ObjectProvider<HealthCheckHandler> healthCheckHandler) {
			return EurekaRegistration.builder(instanceConfig).with(applicationInfoManager)
					.with(eurekaClient).with(healthCheckHandler).build();
		}

	}

	@Configuration
	@ConditionalOnRefreshScope
	protected static class RefreshableEurekaClientConfiguration {

		@Autowired
		private ApplicationContext context;

		@Autowired
		private AbstractDiscoveryClientOptionalArgs<?> optionalArgs;

		@Bean(destroyMethod = "shutdown")
		@ConditionalOnMissingBean(value = EurekaClient.class,
				search = SearchStrategy.CURRENT)
		@org.springframework.cloud.context.config.annotation.RefreshScope
		@Lazy
		public EurekaClient eurekaClient(ApplicationInfoManager manager,
				EurekaClientConfig config, EurekaInstanceConfig instance,
				@Autowired(required = false) HealthCheckHandler healthCheckHandler) {
			// If we use the proxy of the ApplicationInfoManager we could run into a
			// problem
			// when shutdown is called on the CloudEurekaClient where the
			// ApplicationInfoManager bean is
			// requested but wont be allowed because we are shutting down. To avoid this
			// we use the
			// object directly.
			ApplicationInfoManager appManager;
			if (AopUtils.isAopProxy(manager)) {
				appManager = ProxyUtils.getTargetObject(manager);
			}
			else {
				appManager = manager;
			}
			CloudEurekaClient cloudEurekaClient = new CloudEurekaClient(appManager,
					config, this.optionalArgs, this.context);
			cloudEurekaClient.registerHealthCheck(healthCheckHandler);
			return cloudEurekaClient;
		}

		@Bean
		@ConditionalOnMissingBean(value = ApplicationInfoManager.class,
				search = SearchStrategy.CURRENT)
		@org.springframework.cloud.context.config.annotation.RefreshScope
		@Lazy
		public ApplicationInfoManager eurekaApplicationInfoManager(
				EurekaInstanceConfig config) {
			InstanceInfo instanceInfo = new InstanceInfoFactory().create(config);
			return new ApplicationInfoManager(config, instanceInfo);
		}

		@Bean
		@org.springframework.cloud.context.config.annotation.RefreshScope
		@ConditionalOnBean(AutoServiceRegistrationProperties.class)
		@ConditionalOnProperty(
				value = "spring.cloud.service-registry.auto-registration.enabled",
				matchIfMissing = true)
		public EurekaRegistration eurekaRegistration(EurekaClient eurekaClient,
				CloudEurekaInstanceConfig instanceConfig,
				ApplicationInfoManager applicationInfoManager, @Autowired(
						required = false) ObjectProvider<HealthCheckHandler> healthCheckHandler) {
			return EurekaRegistration.builder(instanceConfig).with(applicationInfoManager)
					.with(eurekaClient).with(healthCheckHandler).build();
		}

	}

	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Conditional(OnMissingRefreshScopeCondition.class)
	@interface ConditionalOnMissingRefreshScope {

	}

	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@ConditionalOnClass(RefreshScope.class)
	@ConditionalOnBean(RefreshAutoConfiguration.class)
	@ConditionalOnProperty(value = "eureka.client.refresh.enable", havingValue = "true",
			matchIfMissing = true)
	@interface ConditionalOnRefreshScope {

	}

	private static class OnMissingRefreshScopeCondition extends AnyNestedCondition {

		OnMissingRefreshScopeCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnMissingClass("org.springframework.cloud.context.scope.refresh.RefreshScope")
		static class MissingClass {

		}

		@ConditionalOnMissingBean(RefreshAutoConfiguration.class)
		static class MissingScope {

		}

		@ConditionalOnProperty(value = "eureka.client.refresh.enable",
				havingValue = "false")
		static class OnPropertyDisabled {

		}

	}

	@Configuration
	@ConditionalOnClass(Health.class)
	protected static class EurekaHealthIndicatorConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnEnabledHealthIndicator("eureka")
		public EurekaHealthIndicator eurekaHealthIndicator(EurekaClient eurekaClient,
				EurekaInstanceConfig instanceConfig, EurekaClientConfig clientConfig) {
			return new EurekaHealthIndicator(eurekaClient, instanceConfig, clientConfig);
		}

	}

}
