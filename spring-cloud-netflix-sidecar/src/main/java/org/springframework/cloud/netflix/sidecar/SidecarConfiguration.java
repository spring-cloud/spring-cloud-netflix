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

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import static org.springframework.cloud.commons.util.IdUtils.getDefaultInstanceId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.cloud.netflix.eureka.metadata.DefaultManagementMetadataProvider;
import org.springframework.cloud.netflix.eureka.metadata.ManagementMetadata;
import org.springframework.cloud.netflix.eureka.metadata.ManagementMetadataProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.StringUtils;

import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.discovery.EurekaClientConfig;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

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
 * @author Fabrizio Di Napoli
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

		@Value(value = "${management.port:${MANAGEMENT_PORT:#{null}}}")
		private Integer managementPort;

		@Value("${server.port:${SERVER_PORT:${PORT:8080}}}")
		private int serverPort = 8080;

		@Value("${management.context-path:${MANAGEMENT_CONTEXT_PATH:#{null}}}")
		private String managementContextPath;

		@Value("${server.context-path:${SERVER_CONTEXT_PATH:/}}")
		private String serverContextPath = "/";

		@Value("${eureka.instance.hostname:${EUREKA_INSTANCE_HOSTNAME:}}")
		private String hostname;

		@Autowired
		private ConfigurableEnvironment env;

		@Bean
		@ConditionalOnMissingBean
		public ManagementMetadataProvider serviceManagementMetadataProvider() {
			return new DefaultManagementMetadataProvider();
		}

		@Bean
		public EurekaInstanceConfigBean eurekaInstanceConfigBean(ManagementMetadataProvider managementMetadataProvider) {
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
			ManagementMetadata metadata = managementMetadataProvider.get(config, serverPort,
					serverContextPath, managementContextPath, managementPort);

			if(metadata != null) {
				config.setStatusPageUrl(metadata.getStatusPageUrl());
				config.setHealthCheckUrl(metadata.getHealthCheckUrl());
				if(config.isSecurePortEnabled()) {
					config.setSecureHealthCheckUrl(metadata.getSecureHealthCheckUrl());
				}
				Map<String, String> metadataMap = config.getMetadataMap();
				if (metadataMap.get("management.port") == null) {
					metadataMap.put("management.port", String.valueOf(metadata.getManagementPort()));
				}
			}
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
	@ConditionalOnMissingClass("org.apache.http.client.HttpClient")
	public RestTemplate restTemplate() {
		return new RestTemplateBuilder().build();
	}

	@Bean
	@ConditionalOnClass(HttpClient.class)
	public RestTemplate sslRestTemplate(SidecarProperties properties) {
		RestTemplateBuilder builder = new RestTemplateBuilder();
		if(properties.acceptAllSslCertificates()) {
			CloseableHttpClient httpClient = HttpClients.custom()
					.setSSLHostnameVerifier(new NoopHostnameVerifier())
					.build();
			HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
			requestFactory.setHttpClient(httpClient);
			builder = builder.requestFactory(() -> requestFactory);
		}
		return builder.build();
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
