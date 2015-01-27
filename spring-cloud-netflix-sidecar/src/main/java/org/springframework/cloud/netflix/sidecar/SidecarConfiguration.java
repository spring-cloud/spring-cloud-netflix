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
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.discovery.EurekaClientConfig;

/**
 * @author Spencer Gibb
 */
@Configuration
@EnableConfigurationProperties
@ConditionalOnProperty(value = "spring.cloud.netflix.sidecar.enabled", matchIfMissing = true)
public class SidecarConfiguration {

	@Bean
	public SidecarProperties sidecarProperties() {
		return new SidecarProperties();
	}

	@Configuration
	@ConditionalOnClass(EurekaClientConfig.class)
	protected static class EurekaInstanceConfigBeanConfiguration {
		@Autowired
		private SidecarProperties sidecarProperties;

		@Value("${server.port:${SERVER_PORT:${PORT:8080}}}")
		private int serverPort = 8080;

		@Bean
		public EurekaInstanceConfigBean eurekaInstanceConfigBean() {
			EurekaInstanceConfigBean config = new EurekaInstanceConfigBean();
			int port = sidecarProperties.getPort();
			config.setNonSecurePort(port);
			String scheme = config.getSecurePortEnabled() ? "https" : "http";
			config.setStatusPageUrl(scheme + "://" + config.getHostname() + ":"
					+ this.serverPort + config.getStatusPageUrlPath());
			config.setHealthCheckUrl(scheme + "://" + config.getHostname() + ":"
					+ this.serverPort + config.getHealthCheckUrlPath());
			config.setHomePageUrl(scheme + "://" + config.getHostname() + ":" + port
					+ config.getHomePageUrlPath());
			return config;
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
