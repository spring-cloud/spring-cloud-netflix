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

import lombok.extern.apachecommons.CommonsLog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.cloud.client.discovery.event.HeartbeatMonitor;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SmartApplicationListener;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;

/**
 * Bootstrap configuration for a config client that wants to lookup the config server via
 * discovery.
 *
 * @author Dave Syer
 */
@ConditionalOnClass({ EurekaClient.class, ConfigServicePropertySourceLocator.class })
@ConditionalOnProperty(value = "spring.cloud.config.discovery.enabled", matchIfMissing = false)
@Configuration
@EnableDiscoveryClient
@Import(EurekaClientAutoConfiguration.class)
@CommonsLog
public class DiscoveryClientConfigServiceBootstrapConfiguration implements
		SmartApplicationListener {

	private HeartbeatMonitor monitor = new HeartbeatMonitor();

	@Autowired
	private ConfigClientProperties config;

	@Autowired
	private EurekaClient eurekaClient;

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ContextRefreshedEvent) {
			refresh();
		}
		else if (event instanceof HeartbeatEvent) {
			if (this.monitor.update(((HeartbeatEvent) event).getValue())) {
				refresh();
			}
		}
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return ContextRefreshedEvent.class.isAssignableFrom(eventType)
				|| HeartbeatEvent.class.isAssignableFrom(eventType);
	}

	@Override
	public boolean supportsSourceType(Class<?> sourceType) {
		return true;
	}

	private void refresh() {
		try {
			log.info("Locating configserver via discovery");
			InstanceInfo server = eurekaClient
					.getNextServerFromEureka(this.config.getDiscovery().getServiceId(),
							false);
			String url = server.getHomePageUrl();
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

}
