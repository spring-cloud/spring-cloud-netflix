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

package org.springframework.cloud.netflix.eureka.server;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.config.ConfigurationManager;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.converters.JsonXStream;
import com.netflix.discovery.converters.XmlXStream;
import com.netflix.eureka.EurekaBootStrap;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.EurekaServerContextHolder;
import com.netflix.eureka.V1AwareInstanceInfoConverter;
import com.netflix.eureka.aws.AwsBinderDelegate;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import com.netflix.eureka.util.EurekaMonitors;
import com.thoughtworks.xstream.XStream;

import lombok.extern.apachecommons.CommonsLog;

/**
 * @author Spencer Gibb
 */
@CommonsLog
public class EurekaServerBootstrap extends EurekaBootStrap {
	private static final String TEST = "test";

	private static final String ARCHAIUS_DEPLOYMENT_ENVIRONMENT = "archaius.deployment.environment";

	private static final String EUREKA_ENVIRONMENT = "eureka.environment";

	private static final String DEFAULT = "default";

	private static final String ARCHAIUS_DEPLOYMENT_DATACENTER = "archaius.deployment.datacenter";

	private static final String EUREKA_DATACENTER = "eureka.datacenter";

	private EurekaServerConfig eurekaServerConfig;

	private ApplicationInfoManager applicationInfoManager;

	private EurekaClientConfig eurekaClientConfig;

	private PeerAwareInstanceRegistry registry;

	public EurekaServerBootstrap(ApplicationInfoManager applicationInfoManager, EurekaClientConfig eurekaClientConfig, EurekaServerConfig eurekaServerConfig, PeerAwareInstanceRegistry registry, EurekaServerContext serverContext) {
		this.applicationInfoManager = applicationInfoManager;
		this.eurekaClientConfig = eurekaClientConfig;
		this.eurekaServerConfig = eurekaServerConfig;
		this.registry = registry;
		this.serverContext = serverContext;
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		//NO-OP so it doesn't get initialzed by servlet container, we want spring to init
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		//NO-OP so it doesn't get destroyed by servlet container, we want spring to destroy
	}

	public void contextInitialized(ServletContext context) {
		try {
			initEurekaEnvironment();
			initEurekaServerContext();

			context.setAttribute(EurekaServerContext.class.getName(), serverContext);
		} catch (Throwable e) {
			log.error("Cannot bootstrap eureka server :", e);
			throw new RuntimeException("Cannot bootstrap eureka server :", e);
		}
	}

	public void contextDestroyed(ServletContext context) {
		try {
			log.info("Shutting down Eureka Server..");
			context.removeAttribute(EurekaServerContext.class.getName());

			destroyEurekaServerContext();
			destroyEurekaEnvironment();

		} catch (Throwable e) {
			log.error("Error shutting down eureka", e);
		}
		log.info("Eureka Service is now shutdown...");
	}

	@Override
	protected void initEurekaEnvironment() throws Exception {
		log.info("Setting the eureka configuration..");

		String dataCenter = ConfigurationManager.getConfigInstance().getString(EUREKA_DATACENTER);
		if (dataCenter == null) {
			log.info("Eureka data center value eureka.datacenter is not set, defaulting to default");
			ConfigurationManager.getConfigInstance().setProperty(ARCHAIUS_DEPLOYMENT_DATACENTER, DEFAULT);
		} else {
			ConfigurationManager.getConfigInstance().setProperty(ARCHAIUS_DEPLOYMENT_DATACENTER, dataCenter);
		}
		String environment = ConfigurationManager.getConfigInstance().getString(EUREKA_ENVIRONMENT);
		if (environment == null) {
			ConfigurationManager.getConfigInstance().setProperty(ARCHAIUS_DEPLOYMENT_ENVIRONMENT, TEST);
			log.info("Eureka environment value eureka.environment is not set, defaulting to test");
		}
	}

	@Override
	protected void initEurekaServerContext() throws Exception {
		// For backward compatibility
		JsonXStream.getInstance().registerConverter(new V1AwareInstanceInfoConverter(), XStream.PRIORITY_VERY_HIGH);
		XmlXStream.getInstance().registerConverter(new V1AwareInstanceInfoConverter(), XStream.PRIORITY_VERY_HIGH);

		if (isAws(applicationInfoManager.getInfo())) {
			awsBinder = new AwsBinderDelegate(eurekaServerConfig, eurekaClientConfig, registry, applicationInfoManager);
			awsBinder.start();
		}

		EurekaServerContextHolder.initialize(serverContext);

		log.info("Initialized server context");

		// Copy registry from neighboring eureka node
		int registryCount = registry.syncUp();
		registry.openForTraffic(applicationInfoManager, registryCount);

		// Register all monitoring statistics.
		EurekaMonitors.registerAllStats();
	}
}
