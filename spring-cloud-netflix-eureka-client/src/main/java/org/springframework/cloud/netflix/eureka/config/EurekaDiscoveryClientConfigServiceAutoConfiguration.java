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

package org.springframework.cloud.netflix.eureka.config;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClientConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

import com.netflix.discovery.EurekaClient;

/**
 * Bootstrap configuration for a config client that wants to lookup the config server via
 * discovery.
 *
 * @author Dave Syer
 */
@ConditionalOnBean({ EurekaDiscoveryClientConfiguration.class })
@ConditionalOnProperty(value = "spring.cloud.config.discovery.enabled", matchIfMissing = false)
public class EurekaDiscoveryClientConfigServiceAutoConfiguration {

	@Autowired
	private ConfigurableApplicationContext context;

	@PostConstruct
	public void init() {
		if (this.context.getParent() != null) {
			if (this.context.getBeanNamesForType(EurekaClient.class).length > 0
					&& this.context.getParent()
							.getBeanNamesForType(EurekaClient.class).length > 0) {
				// If the parent has a EurekaClient as well it should be shutdown, so the
				// local one can register accurate instance info
				this.context.getParent().getBean(EurekaClient.class).shutdown();
			}
		}
	}

}
