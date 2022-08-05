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

package org.springframework.cloud.netflix.eureka.config;

import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.discovery.EurekaClient;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Extra configuration for config server if it happens to be a Eureka instance.
 *
 * @author Dave Syer
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties
@ConditionalOnClass(value = { EurekaInstanceConfigBean.class, EurekaClient.class },
		name = "org.springframework.cloud.config.server.config.ConfigServerProperties")
public class EurekaClientConfigServerAutoConfiguration {

	@Autowired(required = false)
	private EurekaInstanceConfig instance;

	@Autowired
	private Environment env;

	@PostConstruct
	public void init() {
		if (this.instance == null) {
			return;
		}
		String prefix = this.env.getProperty("spring.cloud.config.server.prefix");
		if (StringUtils.hasText(prefix) && !StringUtils.hasText(this.instance.getMetadataMap().get("configPath"))) {
			this.instance.getMetadataMap().put("configPath", prefix);
		}
	}

}
