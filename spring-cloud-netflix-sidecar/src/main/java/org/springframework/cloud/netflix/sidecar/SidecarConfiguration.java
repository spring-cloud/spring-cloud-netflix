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

package org.springframework.cloud.netflix.sidecar;

import static org.springframework.cloud.commons.util.IdUtils.getDefaultInstanceId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.discovery.EurekaClientConfig;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Sidecar Configuration that setting up {@link com.netflix.appinfo.EurekaInstanceConfig}.
 * <p>
 * Depends on {@link SidecarProperties} and {@code eureka.instance.hostname} property. Since there is two way to
 * configure hostname:
 * <ol>
 *   <li>{@code eureka.instance.hostname} property</li>
 *   <li>{@link SidecarProperties#hostname}</li>
 * </ol>
 * {@code eureka.instance.hostname} will always win against {@link SidecarProperties#hostname} due to
 * {@code @ConfigurationProperties("eureka.instance")} on {@link EurekaInstanceConfigBeanConfiguration}.
 *
 * @author Spencer Gibb
 * @author Ryan Baxter
 *
 * @see EurekaInstanceConfigBeanConfiguration
 */
@Configuration
@EnableConfigurationProperties
@ConditionalOnProperty(value = "spring.cloud.netflix.sidecar.enabled", matchIfMissing = true)
public class SidecarConfiguration {

	@Bean
	public HasFeatures Feature() {
		return HasFeatures.namedFeature("Netflix Sidecar", SidecarConfiguration.class);
	}

	@Bean
	public SidecarProperties sidecarProperties() {
		return new SidecarProperties();
	}

	@Configuration
	@ConditionalOnClass(EurekaClientConfig.class)
	protected static class EurekaInstanceConfigBeanConfiguration {
		@Autowired
		private SidecarProperties sidecarProperties;

		@Autowired
		private InetUtils inetUtils;

		@Value("${management.port:${MANAGEMENT_PORT:${server.port:${SERVER_PORT:${PORT:8080}}}}}")
		private int managementPort = 8080;

		@Value("${eureka.instance.hostname:${EUREKA_INSTANCE_HOSTNAME:}}")
		private String hostname;

		@Autowired
		private ConfigurableEnvironment env;

		@Bean
		public EurekaInstanceConfigBean eurekaInstanceConfigBean() {
			EurekaInstanceConfigBean config = new EurekaInstanceConfigBean(inetUtils);
			String springAppName = this.env.getProperty("spring.application.name", "");
			int port = this.sidecarProperties.getPort();
			config.setNonSecurePort(port);
			config.setInstanceId(getDefaultInstanceId(this.env));
			if (StringUtils.hasText(springAppName)) {
				config.setAppname(springAppName);
				config.setVirtualHostName(springAppName);
				config.setSecureVirtualHostName(springAppName);
			}
			String hostname = this.sidecarProperties.getHostname();
			String ipAddress = this.sidecarProperties.getIpAddress();
			if (!StringUtils.hasText(hostname) && StringUtils.hasText(this.hostname)) {
				hostname = this.hostname;
			}
			if (StringUtils.hasText(hostname)) {
				config.setHostname(hostname);
			}
			if (StringUtils.hasText(ipAddress)) {
				config.setIpAddress(ipAddress);
			}
			String scheme = config.getSecurePortEnabled() ? "https" : "http";
			config.setStatusPageUrl(scheme + "://" + config.getHostname() + ":"
					+ this.managementPort + config.getStatusPageUrlPath());
			config.setHealthCheckUrl(scheme + "://" + config.getHostname() + ":"
					+ this.managementPort + config.getHealthCheckUrlPath());
			config.setHomePageUrl(scheme + "://" + config.getHostname() + ":" + port
					+ config.getHomePageUrlPath());
			return config;
		}

		@Bean
		public HealthCheckHandler healthCheckHandler(
				final LocalApplicationHealthIndicator healthIndicator) {
			return new LocalApplicationHealthCheckHandler(healthIndicator);
		}

	}

	@Bean
	public LocalApplicationHealthIndicator localApplicationHealthIndicator() {
		return new LocalApplicationHealthIndicator();
	}

	@Bean
	public SidecarController sidecarController() {
		return new SidecarController();
	}

}
