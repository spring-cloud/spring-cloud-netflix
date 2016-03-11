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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

import static org.springframework.cloud.commons.util.IdUtils.getDefaultInstanceId;

/**
 * @author Spencer Gibb
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
			int port = this.sidecarProperties.getPort();
			config.setNonSecurePort(port);
			config.setInstanceId(getDefaultInstanceId(this.env));
			if (StringUtils.hasText(this.hostname)) {
				config.setHostname(this.hostname);
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
