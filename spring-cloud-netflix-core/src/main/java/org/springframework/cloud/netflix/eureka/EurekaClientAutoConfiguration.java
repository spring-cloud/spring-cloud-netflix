/*
 * Copyright 2013-2014 the original author or authors.
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

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.CommonsClientAutoConfiguration;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.noop.NoopDiscoveryClientAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.converters.JsonXStream;
import com.netflix.discovery.converters.XmlXStream;

import lombok.SneakyThrows;

/**
 * @author Dave Syer
 */
@Configuration
@EnableConfigurationProperties
@ConditionalOnClass(EurekaClientConfig.class)
@ConditionalOnProperty(value = "eureka.client.enabled", matchIfMissing = true)
@AutoConfigureBefore({ NoopDiscoveryClientAutoConfiguration.class,
		CommonsClientAutoConfiguration.class })
public class EurekaClientAutoConfiguration {

	@Autowired
	private ApplicationContext context;

	@Value("${server.port:${SERVER_PORT:${PORT:8080}}}")
	int nonSecurePort;

	@PostConstruct
	public void init() {
		DataCenterAwareJacksonCodec.init();
		XmlXStream.getInstance()
				.setMarshallingStrategy(new DataCenterAwareMarshallingStrategy());
		JsonXStream.getInstance()
				.setMarshallingStrategy(new DataCenterAwareMarshallingStrategy());
	}

	@Bean
	@ConditionalOnMissingBean(EurekaClientConfig.class)
	public EurekaClientConfigBean eurekaClientConfigBean() {
		return new EurekaClientConfigBean();
	}

	@Bean
	@ConditionalOnMissingBean(EurekaInstanceConfig.class)
	public EurekaInstanceConfigBean eurekaInstanceConfigBean() {
		EurekaInstanceConfigBean instance = new EurekaInstanceConfigBean();
		instance.setNonSecurePort(this.nonSecurePort);
		return instance;
	}

	@Bean
	@ConditionalOnMissingBean(EurekaClient.class)
	@SneakyThrows
	public EurekaClient eurekaClient(ApplicationInfoManager applicationInfoManager,
			EurekaClientConfig config) {
		return new CloudEurekaClient(applicationInfoManager, config, this.context);
	}

	@Bean
	@ConditionalOnMissingBean(ApplicationInfoManager.class)
	public ApplicationInfoManager applicationInfoManager(EurekaInstanceConfig config,
			InstanceInfo instanceInfo) {
		return new ApplicationInfoManager(config, instanceInfo);
	}

	@Bean
	@ConditionalOnMissingBean(InstanceInfo.class)
	public InstanceInfo instanceInfo(EurekaInstanceConfig config) {
		return new InstanceInfoFactory().create(config);
	}

	@Bean
	public DiscoveryClient discoveryClient(EurekaInstanceConfig config,
			EurekaClient client) {
		return new EurekaDiscoveryClient(config, client);
	}

}
