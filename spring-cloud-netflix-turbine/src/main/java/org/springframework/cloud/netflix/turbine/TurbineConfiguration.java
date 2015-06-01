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

package org.springframework.cloud.netflix.turbine;

import com.netflix.discovery.EurekaClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import com.netflix.turbine.discovery.InstanceDiscovery;
import com.netflix.turbine.init.TurbineInit;
import com.netflix.turbine.plugins.PluginsFactory;
import com.netflix.turbine.streaming.servlet.TurbineStreamServlet;

/**
 * @author Spencer Gibb
 */
@Configuration
@EnableConfigurationProperties
@EnableDiscoveryClient
public class TurbineConfiguration implements SmartLifecycle, Ordered {

	@Autowired
	private EurekaClient eurekaClient;

	@Bean
	public ServletRegistrationBean turbineStreamServlet() {
		return new ServletRegistrationBean(new TurbineStreamServlet(), "/turbine.stream");
	}

	@Bean
	public TurbineProperties turbineProperties() {
		return new TurbineProperties();
	}

	@Bean
	public InstanceDiscovery instanceDiscovery() {
		return new EurekaInstanceDiscovery(turbineProperties(), eurekaClient);
	}

	private boolean running;

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public void stop(Runnable callback) {
		callback.run();
	}

	@Override
	public void start() {
		// TODO: figure out ordering, so this is already run by
		// EurekaDiscoveryClientConfiguration
		// DiscoveryManager.getInstance().initComponent(instanceConfig, clientConfig);
		PluginsFactory.setClusterMonitorFactory(new SpringAggregatorFactory());
		PluginsFactory.setInstanceDiscovery(instanceDiscovery());
		TurbineInit.init();
	}

	@Override
	public void stop() {
		this.running = false;
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public int getPhase() {
		return 0;
	}

	@Override
	public int getOrder() {
		return -1;
	}

}
