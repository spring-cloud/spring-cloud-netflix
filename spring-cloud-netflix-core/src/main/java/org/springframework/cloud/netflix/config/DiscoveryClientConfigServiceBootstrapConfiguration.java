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
package org.springframework.cloud.netflix.config;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.DiscoveryClient;

/**
 * @author Dave Syer
 *
 */
@ConditionalOnClass({ DiscoveryClient.class, ConfigServicePropertySourceLocator.class })
@ConditionalOnExpression("${spring.cloud.config.discovery.enabled:false}")
@Configuration
@EnableEurekaClient
@Import(EurekaClientAutoConfiguration.class)
@Slf4j
public class DiscoveryClientConfigServiceBootstrapConfiguration implements
		ApplicationListener<ContextRefreshedEvent> {

	@Autowired
	private DiscoveryClient client;
	
	@Autowired
	private ConfigClientProperties config;

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		try {
			log.info("Locating configserver via discovery");
			Environment environment = event.getApplicationContext().getEnvironment();
			if (!(environment instanceof ConfigurableEnvironment)) {
				log.info("Environment is not ConfigurableEnvironment so cannot look up configserver");
				return;
			}
			InstanceInfo server = client.getNextServerFromEureka(config.getDiscovery().getServiceId(),
					false);
			String url = server.getHomePageUrl();
			if (server.getMetadata().containsKey("password")) {
				String user = server.getMetadata().get("user");
				user = user == null ? "user" : user;
				config.setUsername(user);
				String password = server.getMetadata().get("password");
				config.setPassword(password);
			}
			config.setUri(url);
		}
		catch (Exception e) {
			log.warn("Could not locate configserver via discovery", e);
		}
	}

}
