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
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.DiscoveryClient;

/**
 * @author Dave Syer
 *
 */
@ConditionalOnClass({DiscoveryClient.class, ConfigServicePropertySourceLocator.class})
@ConditionalOnExpression("${spring.cloud.bootstrap.config.useDiscovery:false}")
@Configuration
@EnableEurekaClient
@Import(EurekaClientAutoConfiguration.class)
@Slf4j
public class DiscoveryClientConfigServiceBootstrapConfiguration implements ApplicationListener<ContextRefreshedEvent> {
	
	private static final String DEFAULT_CONFIG_SERVER = "CONFIGSERVER";

	@Autowired
	private DiscoveryClient client;
	
	@Autowired
	private ConfigServicePropertySourceLocator delegate;

	
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		try {
			log.info("Locating configserver via discovery");
			InstanceInfo server = client.getNextServerFromEureka(DEFAULT_CONFIG_SERVER, false);
			delegate.setUri(server.getHomePageUrl());
		} catch (Exception e) {
			log.warn("Could not locate configserver via discovery", e);
		}		
	}

}
