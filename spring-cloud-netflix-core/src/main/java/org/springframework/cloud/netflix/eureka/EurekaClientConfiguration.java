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

import javax.annotation.PreDestroy;
import javax.management.MBeanServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.servo.ServoMetricReader;
import org.springframework.context.ApplicationListener;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.DiscoveryManager;
import com.netflix.discovery.EurekaClientConfig;

/**
 * @author Dave Syer
 *
 */
@Configuration
// TODO: make these beans @Conditional
@EnableConfigurationProperties({ EurekaClientConfigBean.class,
		EurekaInstanceConfigBean.class })
@ConditionalOnClass(EurekaClientConfig.class)
@ConditionalOnExpression("${eureka.client.enabled:true}")
public class EurekaClientConfiguration implements
		ApplicationListener<ContextRefreshedEvent>, SmartLifecycle, Ordered {

	private static final Logger logger = LoggerFactory
			.getLogger(EurekaClientConfiguration.class);

	private boolean running;

	private int order = 0;

	@Autowired
	private EurekaClientConfigBean clientConfig;

	@Autowired
	private EurekaInstanceConfigBean instanceConfig;

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		logger.info("Registering application {} with eureka with status UP",
				instanceConfig.getAppname());
		ApplicationInfoManager.getInstance().setInstanceStatus(InstanceStatus.UP);
	}

	@PreDestroy
	public void close() {
		logger.info("Removing application {} from eureka", instanceConfig.getAppname());
		DiscoveryManager.getInstance().shutdownComponent();
	}

	@Override
	public void start() {
		DiscoveryManager.getInstance().initComponent(instanceConfig, clientConfig);
	}

	@Override
	public void stop() {
		running = false;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public int getPhase() {
		return 0;
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public void stop(Runnable callback) {
		callback.run();
	}

	@Override
	public int getOrder() {
		return order;
	}

	@Bean
	@Lazy
	@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
	public DiscoveryClient discoveryClient() {
		return DiscoveryManager.getInstance().getDiscoveryClient();
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(MBeanServer.class)
	public EurekaHealthIndicator eurekaHealthIndicator(MBeanServer server,
			EurekaInstanceConfig config) {
		return new EurekaHealthIndicator(discoveryClient(),
				new ServoMetricReader(server), config);
	}

}
