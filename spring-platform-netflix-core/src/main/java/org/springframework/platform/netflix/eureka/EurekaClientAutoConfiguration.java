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
package org.springframework.platform.netflix.eureka;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.discovery.DiscoveryManager;
import com.netflix.discovery.EurekaClientConfig;

/**
 * @author Dave Syer
 *
 */
@Configuration
@EnableConfigurationProperties({ EurekaClientConfigBean.class,
		EurekaInstanceConfigBean.class })
@ConditionalOnClass(EurekaClientConfig.class)
@ConditionalOnExpression("${eureka.client.enabled:true}")
public class EurekaClientAutoConfiguration implements
		ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(EurekaClientAutoConfiguration.class);

	@Autowired
	private EurekaClientConfigBean clientConfig;

	@Autowired
	private EurekaInstanceConfigBean instanceConfig;

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
        logger.info("Registering application {} with eureka with status UP", instanceConfig.getAppname());
		ApplicationInfoManager.getInstance().setInstanceStatus(InstanceStatus.UP);
	}

	@PostConstruct
	public void init() {
		DiscoveryManager.getInstance().initComponent(instanceConfig, clientConfig);
	}

	@PreDestroy
	public void close() {
        logger.info("Removing application {} from eureka", instanceConfig.getAppname());
		DiscoveryManager.getInstance().shutdownComponent();
	}

}
