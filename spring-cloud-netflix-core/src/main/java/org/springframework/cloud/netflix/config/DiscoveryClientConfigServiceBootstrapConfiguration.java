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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;

import lombok.extern.apachecommons.CommonsLog;

/**
 * Bootstrap configuration for a config client that wants to lookup the config server via
 * discovery.
 *
 * @author Dave Syer
 */
@ConditionalOnClass({ EurekaClient.class, ConfigServicePropertySourceLocator.class })
@ConditionalOnProperty(value = "spring.cloud.config.discovery.enabled", matchIfMissing = false)
@Configuration
@Import(EurekaClientAutoConfiguration.class)
@CommonsLog
public class DiscoveryClientConfigServiceBootstrapConfiguration {

	@Autowired
	private ConfigClientProperties config;

	@Autowired
	private DiscoveryClient client;

	@Autowired
	private EurekaClient eurekaClient;

	@EventListener(ContextRefreshedEvent.class)
	public void onApplicationEvent(ContextRefreshedEvent event) {
		refresh();
	}

	// TODO: re-instate heart beat (maybe? isn't it handled in the child context?)

	private void refresh() {
		try {
			log.debug("Locating configserver via discovery");
			InstanceInfo server = this.eurekaClient.getNextServerFromEureka(
					this.config.getDiscovery().getServiceId(), false);
			String url = getHomePage(server);
			if (server.getMetadata().containsKey("password")) {
				String user = server.getMetadata().get("user");
				user = user == null ? "user" : user;
				this.config.setUsername(user);
				String password = server.getMetadata().get("password");
				this.config.setPassword(password);
			}
			if (server.getMetadata().containsKey("configPath")) {
				String path = server.getMetadata().get("configPath");
				if (url.endsWith("/") && path.startsWith("/")) {
					url = url.substring(0, url.length() - 1);
				}
				url = url + path;
			}
			this.config.setUri(url);
		}
		catch (Exception ex) {
			log.warn("Could not locate configserver via discovery", ex);
		}
	}

	private String getHomePage(InstanceInfo server) {
		List<ServiceInstance> instances = this.client
				.getInstances(this.config.getDiscovery().getServiceId());
		if (instances == null || instances.isEmpty()) {
			return server.getHomePageUrl();
		}
		return instances.get(0).getUri().toString() + "/";
	}

}
