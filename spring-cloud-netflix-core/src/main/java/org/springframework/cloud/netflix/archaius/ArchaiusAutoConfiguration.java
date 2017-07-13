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

package org.springframework.cloud.netflix.archaius;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PreDestroy;

import com.netflix.config.AggregatedConfiguration;
import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DeploymentContext;
import com.netflix.config.DynamicProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicURLConfiguration;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.ConfigurationBuilder;
import org.apache.commons.configuration.EnvironmentConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.util.ReflectionUtils;

import static com.netflix.config.ConfigurationManager.APPLICATION_PROPERTIES;
import static com.netflix.config.ConfigurationManager.DISABLE_DEFAULT_ENV_CONFIG;
import static com.netflix.config.ConfigurationManager.DISABLE_DEFAULT_SYS_CONFIG;
import static com.netflix.config.ConfigurationManager.ENV_CONFIG_NAME;
import static com.netflix.config.ConfigurationManager.SYS_CONFIG_NAME;
import static com.netflix.config.ConfigurationManager.URL_CONFIG_NAME;

/**
 * @author Spencer Gibb
 */
@Configuration
@ConditionalOnClass({ ConcurrentCompositeConfiguration.class,
		ConfigurationBuilder.class })
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class ArchaiusAutoConfiguration {

	private static final Log log = LogFactory.getLog(ArchaiusAutoConfiguration.class);

	private static final AtomicBoolean initialized = new AtomicBoolean(false);

	@Autowired
	private ConfigurableEnvironment env;

	@Autowired(required = false)
	private List<AbstractConfiguration> externalConfigurations = new ArrayList<>();

	private DynamicURLConfiguration defaultURLConfig;

	@PreDestroy
	public void close() {
		if (defaultURLConfig != null) {
			defaultURLConfig.stopLoading();
		}
		setStatic(ConfigurationManager.class, "instance", null);
		setStatic(ConfigurationManager.class, "customConfigurationInstalled", false);
		setStatic(DynamicPropertyFactory.class, "config", null);
		setStatic(DynamicPropertyFactory.class, "initializedWithDefaultConfig", false);
		setStatic(DynamicProperty.class, "dynamicPropertySupportImpl", null);
		initialized.compareAndSet(true, false);
	}

	@Bean
	public ConfigurableEnvironmentConfiguration configurableEnvironmentConfiguration() {
		ConfigurableEnvironmentConfiguration envConfig = new ConfigurableEnvironmentConfiguration(
				this.env);
		configureArchaius(envConfig);
		return envConfig;
	}

	@Configuration
	@ConditionalOnClass(Endpoint.class)
	@ConditionalOnEnabledEndpoint("archaius")
	protected static class ArchaiusEndpointConfiguration {
		@Bean
		protected ArchaiusEndpoint archaiusEndpoint() {
			return new ArchaiusEndpoint();
		}
	}

	@Configuration
	@ConditionalOnProperty(value = "archaius.propagate.environmentChangedEvent", matchIfMissing = true)
	@ConditionalOnClass(EnvironmentChangeEvent.class)
	protected static class PropagateEventsConfiguration
			implements ApplicationListener<EnvironmentChangeEvent> {
		@Autowired
		private Environment env;

		@Override
		public void onApplicationEvent(EnvironmentChangeEvent event) {
			AbstractConfiguration manager = ConfigurationManager.getConfigInstance();
			for (String key : event.getKeys()) {
				for (ConfigurationListener listener : manager
						.getConfigurationListeners()) {
					Object source = event.getSource();
					// TODO: Handle add vs set vs delete?
					int type = AbstractConfiguration.EVENT_SET_PROPERTY;
					String value = this.env.getProperty(key);
					boolean beforeUpdate = false;
					listener.configurationChanged(new ConfigurationEvent(source, type,
							key, value, beforeUpdate));
				}
			}
		}
	}

	protected void configureArchaius(ConfigurableEnvironmentConfiguration envConfig) {
		if (initialized.compareAndSet(false, true)) {
			String appName = this.env.getProperty("spring.application.name");
			if (appName == null) {
				appName = "application";
				log.warn("No spring.application.name found, defaulting to 'application'");
			}
			System.setProperty(DeploymentContext.ContextKey.appId.getKey(), appName);

			ConcurrentCompositeConfiguration config = new ConcurrentCompositeConfiguration();

			// support to add other Configurations (Jdbc, DynamoDb, Zookeeper, jclouds,
			// etc...)
			if (this.externalConfigurations != null) {
				for (AbstractConfiguration externalConfig : this.externalConfigurations) {
					config.addConfiguration(externalConfig);
				}
			}
			config.addConfiguration(envConfig,
					ConfigurableEnvironmentConfiguration.class.getSimpleName());

			defaultURLConfig = new DynamicURLConfiguration();
			try {
				config.addConfiguration(defaultURLConfig, URL_CONFIG_NAME);
			}
			catch (Throwable ex) {
				log.error("Cannot create config from " + defaultURLConfig, ex);
			}

			// TODO: sys/env above urls?
			if (!Boolean.getBoolean(DISABLE_DEFAULT_SYS_CONFIG)) {
				SystemConfiguration sysConfig = new SystemConfiguration();
				config.addConfiguration(sysConfig, SYS_CONFIG_NAME);
			}
			if (!Boolean.getBoolean(DISABLE_DEFAULT_ENV_CONFIG)) {
				EnvironmentConfiguration environmentConfiguration = new EnvironmentConfiguration();
				config.addConfiguration(environmentConfiguration, ENV_CONFIG_NAME);
			}

			ConcurrentCompositeConfiguration appOverrideConfig = new ConcurrentCompositeConfiguration();
			config.addConfiguration(appOverrideConfig, APPLICATION_PROPERTIES);
			config.setContainerConfigurationIndex(
					config.getIndexOfConfiguration(appOverrideConfig));

			addArchaiusConfiguration(config);
		}
		else {
			// TODO: reinstall ConfigurationManager
			log.warn(
					"Netflix ConfigurationManager has already been installed, unable to re-install");
		}
	}

	private void addArchaiusConfiguration(ConcurrentCompositeConfiguration config) {
		if (ConfigurationManager.isConfigurationInstalled()) {
			AbstractConfiguration installedConfiguration = ConfigurationManager
					.getConfigInstance();
			if (installedConfiguration instanceof ConcurrentCompositeConfiguration) {
				ConcurrentCompositeConfiguration configInstance = (ConcurrentCompositeConfiguration) installedConfiguration;
				configInstance.addConfiguration(config);
			}
			else {
				installedConfiguration.append(config);
				if (!(installedConfiguration instanceof AggregatedConfiguration)) {
					log.warn(
							"Appending a configuration to an existing non-aggregated installed configuration will have no effect");
				}
			}
		}
		else {
			ConfigurationManager.install(config);
		}
	}

	private static void setStatic(Class<?> type, String name, Object value) {
		// Hack a private static field
		Field field = ReflectionUtils.findField(type, name);
		ReflectionUtils.makeAccessible(field);
		ReflectionUtils.setField(field, null, value);
	}

}
