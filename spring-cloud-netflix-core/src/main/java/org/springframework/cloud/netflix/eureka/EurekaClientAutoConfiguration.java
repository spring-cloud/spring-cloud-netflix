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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PostConstruct;

import lombok.SneakyThrows;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.builder.ParentContextApplicationContextInitializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.cloud.client.discovery.event.ParentHeartbeatEvent;
import org.springframework.cloud.client.discovery.noop.NoopDiscoveryClientAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.converters.XmlXStream;

/**
 * @author Dave Syer
 */
@Configuration
@EnableConfigurationProperties
@ConditionalOnClass(EurekaClientConfig.class)
@ConditionalOnProperty(value = "eureka.client.enabled", matchIfMissing = true)
@AutoConfigureBefore(NoopDiscoveryClientAutoConfiguration.class)
public class EurekaClientAutoConfiguration implements ApplicationListener<ParentContextApplicationContextInitializer.ParentContextAvailableEvent> {

	private static final ConcurrentMap<String, String> listenerAdded = new ConcurrentHashMap<>();

	@Autowired
	private ApplicationContext context;

	@Value("${server.port:${SERVER_PORT:${PORT:8080}}}")
	int nonSecurePort;
	
	@PostConstruct
	public void init() {
		DataCenterAwareJacksonCodec.init();
		XmlXStream.getInstance().setMarshallingStrategy(
				new DataCenterAwareMarshallingStrategy());
	}

	@Bean
	@ConditionalOnMissingBean(EurekaClientConfig.class)
	public EurekaClientConfigBean eurekaClientConfigBean() {
		return new EurekaClientConfigBean();
	}

	@Bean
	@ConditionalOnMissingBean(EurekaInstanceConfig.class)
	public EurekaInstanceConfigBean eurekaInstanceConfigBean() {
		EurekaInstanceConfigBean instance = new EurekaInstanceConfigBean();
		instance.setNonSecurePort(this.nonSecurePort);
		return instance;
	}

	@Bean
	@ConditionalOnMissingBean(EurekaClient.class)
	@SneakyThrows
	public EurekaClient eurekaClient() {
		return new CloudEurekaClient(applicationInfoManager(), eurekaClientConfigBean(), context);
	}

	@Bean
	@ConditionalOnMissingBean(ApplicationInfoManager.class)
	public ApplicationInfoManager applicationInfoManager() {
		return new ApplicationInfoManager(eurekaInstanceConfigBean(), instanceInfo());
	}

	@Bean
	@ConditionalOnMissingBean(InstanceInfo.class)
	public InstanceInfo instanceInfo() {
		return new InstanceInfoFactory().create(eurekaInstanceConfigBean());
	}

	@Bean
	public DiscoveryClient discoveryClient() {
		return new EurekaDiscoveryClient();
	}

	/**
	 * propagate HeartbeatEvent from parent to child. Do it via a
	 * ParentHeartbeatEvent since events get published to the parent context,
	 * otherwise results in a stack overflow
	 * @param event
	 */
	@Override
	public void onApplicationEvent(final ParentContextApplicationContextInitializer.ParentContextAvailableEvent event) {
		final ConfigurableApplicationContext context = event.getApplicationContext();
		String childId = context.getId();
		ApplicationContext parent = context.getParent();
		if (parent != null && "bootstrap".equals(parent.getId())
				&& parent instanceof ConfigurableApplicationContext) {
			if (listenerAdded.putIfAbsent(childId, childId) == null) {
				@SuppressWarnings("resource")
				ConfigurableApplicationContext ctx = (ConfigurableApplicationContext) parent;
				ctx.addApplicationListener(new ApplicationListener<HeartbeatEvent>() {
					@Override
					public void onApplicationEvent(HeartbeatEvent dhe) {
						context.publishEvent(new ParentHeartbeatEvent(dhe
								.getSource(), dhe.getValue()));
					}
				});
			}
		}
	}
}
