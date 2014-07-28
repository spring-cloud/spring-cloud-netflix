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

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.context.ServletContextAware;

import com.netflix.blitz4j.LoggingConfiguration;
import com.netflix.eureka.EurekaBootStrap;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.EurekaServerConfigurationManager;
import com.netflix.eureka.PeerAwareInstanceRegistry;

/**
 * @author Dave Syer
 *
 */
@Configuration
@EnableConfigurationProperties(EurekaServerConfigBean.class)
@ConditionalOnClass(EurekaServerConfig.class)
@ConditionalOnExpression("${eureka.server.enabled:true}")
public class EurekaServerAutoConfiguration implements ServletContextAware,
		SmartLifecycle, Ordered {

	@Autowired
	private EurekaServerConfig eurekaServerConfig;

	private ServletContext servletContext;

	@Autowired
	private ApplicationContext applicationContext;

	private boolean running;

	private int order = 0;

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Override
	public void start() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				new EurekaBootStrap() {
					@Override
					protected void initEurekaEnvironment() {
						LoggingConfiguration.getInstance().configure();
						EurekaServerConfigurationManager.getInstance()
								.setConfiguration(eurekaServerConfig);
						PeerAwareInstanceRegistry.getInstance();
						applicationContext.publishEvent(new EurekaRegistryAvailableEvent(eurekaServerConfig));
					}
				}.contextInitialized(new ServletContextEvent(servletContext));
				running = true;
				applicationContext.publishEvent(new EurekaServerStartedEvent(eurekaServerConfig));
			}
		}).start();
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
	
}
