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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.Ordered;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.discovery.DiscoveryManager;
import com.netflix.discovery.EurekaClientConfig;

/**
 * @author Dave Syer
 *
 */
@Configuration
@EnableConfigurationProperties
@ConditionalOnClass(EurekaClientConfig.class)
@ConditionalOnExpression("${eureka.client.enabled:true}")
public class EurekaClientConfiguration implements SmartLifecycle, Ordered {

	private static final Logger logger = LoggerFactory
			.getLogger(EurekaClientConfiguration.class);

	private boolean running;

	private int order = 0;

	private int port = 0;

	@Autowired
	private EurekaClientConfigBean clientConfig;

	@Autowired
	private EurekaInstanceConfigBean instanceConfig;

	@PreDestroy
	public void close() {
		logger.info("Removing application {} from eureka", instanceConfig.getAppname());
		DiscoveryManager.getInstance().shutdownComponent();
	}

	@Override
	public void start() {
        //only set the port if the nonSecurePort is 0 and this.port != 0
		if (port != 0 && instanceConfig.getNonSecurePort() == 0) {
			instanceConfig.setNonSecurePort(port);
        }
        //only initialize if nonSecurePort is greater than 0 and it isn't already running
        //because of containerPortInitializer below
        if (!running && instanceConfig.getNonSecurePort() > 0) {
            discoveryManagerIntitializer().init();
            logger.info("Registering application {} with eureka with status UP",
                    instanceConfig.getAppname());
            ApplicationInfoManager.getInstance().setInstanceStatus(InstanceStatus.UP);
            running = true;
        }
	}

	@Override
	public void stop() {
		logger.info("Unregistering application {} with eureka with status OUT_OF_SERVICE",
				instanceConfig.getAppname());
		ApplicationInfoManager.getInstance().setInstanceStatus(InstanceStatus.OUT_OF_SERVICE);
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
    @ConditionalOnMissingBean(DiscoveryManagerInitializer.class)
    public DiscoveryManagerInitializer discoveryManagerIntitializer() {
        return new DiscoveryManagerInitializer();
    }

	@Bean
	@Lazy
	@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
	@ConditionalOnMissingBean(com.netflix.discovery.DiscoveryClient.class)
	public com.netflix.discovery.DiscoveryClient eurekaDiscoveryClient() {
		return DiscoveryManager.getInstance().getDiscoveryClient();
	}

    @Bean
    public DiscoveryClient discoveryClient() {
        return new EurekaDiscoveryClient();
    }

	@Bean
	protected ApplicationListener<EmbeddedServletContainerInitializedEvent> containerPortInitializer() {
		return new ApplicationListener<EmbeddedServletContainerInitializedEvent>() {

			@Override
			public void onApplicationEvent(EmbeddedServletContainerInitializedEvent event) {
				// TODO: take SSL into account when Spring Boot 1.2 is available
				EurekaClientConfiguration.this.port = event.getEmbeddedServletContainer()
						.getPort();
				EurekaClientConfiguration.this.start();
			}
		};
	}

}
