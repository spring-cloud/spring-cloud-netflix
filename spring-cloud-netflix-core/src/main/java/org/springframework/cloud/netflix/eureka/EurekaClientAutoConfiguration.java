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

import static org.springframework.cloud.util.IdUtils.getDefaultInstanceId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.client.CommonsClientAutoConfiguration;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.client.discovery.noop.NoopDiscoveryClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;

/**
 * @author Dave Syer
 * @author Spencer Gibb
 * @author Jon Schneider
 */
@Configuration
@EnableConfigurationProperties
@ConditionalOnClass(EurekaClientConfig.class)
@ConditionalOnProperty(value = "eureka.client.enabled", matchIfMissing = true)
@AutoConfigureBefore({ NoopDiscoveryClientAutoConfiguration.class,
		CommonsClientAutoConfiguration.class })
@AutoConfigureAfter(RefreshAutoConfiguration.class)
public class EurekaClientAutoConfiguration {

	@Value("${server.port:${SERVER_PORT:${PORT:8080}}}")
	int nonSecurePort;

	@Autowired
	ConfigurableEnvironment env;

	@Bean
	public HasFeatures eurekaFeature() {
		return HasFeatures.namedFeature("Eureka Client", EurekaClient.class);
	}

	@Bean
	@ConditionalOnMissingBean(value = EurekaClientConfig.class, search = SearchStrategy.CURRENT)
	public EurekaClientConfigBean eurekaClientConfigBean() {
		return new EurekaClientConfigBean();
	}

	@Bean
	@ConditionalOnMissingBean(value = EurekaInstanceConfig.class, search = SearchStrategy.CURRENT)
	public EurekaInstanceConfigBean eurekaInstanceConfigBean() {
		EurekaInstanceConfigBean instance = new EurekaInstanceConfigBean();
		instance.setNonSecurePort(this.nonSecurePort);
		instance.setInstanceId(getDefaultInstanceId(env));
		return instance;
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

}
