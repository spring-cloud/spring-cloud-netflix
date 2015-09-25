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

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClientConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.discovery.DiscoveryManager;
import com.netflix.discovery.EurekaClientConfig;

/**
 * Bootstrap configuration for a config client that wants to lookup the config server via
 * discovery.
 *
 * @author Dave Syer
 */
@ConditionalOnBean({ EurekaDiscoveryClientConfiguration.class })
@ConditionalOnProperty(value = "spring.cloud.config.discovery.enabled", matchIfMissing = false)
@Configuration
public class DiscoveryClientConfigServiceAutoConfiguration
		implements ApplicationListener<RefreshScopeRefreshedEvent> {

	@Autowired
	private EurekaClientConfig clientConfig;

	@Autowired
	private EurekaInstanceConfig instanceConfig;

	@Autowired
	private EurekaDiscoveryClientConfiguration lifecycle;

	@PostConstruct
	public void init() {
		this.lifecycle.stop();
		if (DiscoveryManager.getInstance().getDiscoveryClient() != null) {
			DiscoveryManager.getInstance().getDiscoveryClient().shutdown();
		}
		ApplicationInfoManager.getInstance().initComponent(this.instanceConfig);
		DiscoveryManager.getInstance().initComponent(this.instanceConfig,
				this.clientConfig);
		this.lifecycle.start();
	}

	@Override
	public void onApplicationEvent(RefreshScopeRefreshedEvent arg0) {
		init();
	}

}
