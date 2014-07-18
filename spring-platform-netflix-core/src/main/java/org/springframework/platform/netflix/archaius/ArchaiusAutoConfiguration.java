package org.springframework.platform.netflix.archaius;

import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicURLConfiguration;
import org.apache.commons.configuration.EnvironmentConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.netflix.config.ConfigurationBasedDeploymentContext.DEPLOYMENT_APPLICATION_ID_PROPERTY;
import static com.netflix.config.ConfigurationManager.*;
import static com.netflix.config.ConfigurationManager.APPLICATION_PROPERTIES;
import static com.netflix.config.ConfigurationManager.ENV_CONFIG_NAME;

/**
 * Created by sgibb on 7/3/14.
 */
@Configuration
public class ArchaiusAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ArchaiusAutoConfiguration.class);
    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    @Autowired
    ConfigurableEnvironment env;

    @Bean
    ConfigurableEnvironmentConfiguration configurableEnvironmentConfiguration() {
        ConfigurableEnvironmentConfiguration envConfig = new ConfigurableEnvironmentConfiguration(env);
        configureArchaius(envConfig);
        return envConfig;
    }

    protected void configureArchaius(ConfigurableEnvironmentConfiguration envConfig) {
        if (initialized.compareAndSet(false, true)) {
            String appName = env.getProperty("spring.application.name");
            if (appName == null) {
                throw new IllegalStateException("spring.application.name may not be null");
            }
            //this is deprecated, but currently it seams the only way to set it initially
            System.setProperty(DEPLOYMENT_APPLICATION_ID_PROPERTY, appName);

            //TODO: support for other DeploymentContexts

            ConcurrentCompositeConfiguration config = new ConcurrentCompositeConfiguration();

            //support to add other Configurations (Jdbc, DynamoDb, Zookeeper, jclouds, etc...)
            /*if (factories != null && !factories.isEmpty()) {
                for (PropertiesSourceFactory factory: factories) {
                    config.addConfiguration(factory.getConfiguration(), factory.getName());
                }
            }*/
            config.addConfiguration(envConfig, ConfigurableEnvironmentConfiguration.class.getSimpleName());

            //below come from ConfigurationManager.createDefaultConfigInstance()
            try {
                DynamicURLConfiguration defaultURLConfig = new DynamicURLConfiguration();
                config.addConfiguration(defaultURLConfig, URL_CONFIG_NAME);
            } catch (Throwable e) {
                e.printStackTrace(); //TODO: log error
            }

            //TODO: sys/env above urls?
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
            config.setContainerConfigurationIndex(config.getIndexOfConfiguration(appOverrideConfig));

            ConfigurationManager.install(config);
        } else {
            //TODO: reinstall ConfigurationManager
            logger.warn("Netflix ConfigurationManager has already been installed, unable to re-install");
        }
    }
}
