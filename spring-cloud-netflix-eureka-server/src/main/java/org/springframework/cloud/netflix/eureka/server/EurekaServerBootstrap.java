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

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.config.ConfigurationManager;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.converters.JsonXStream;
import com.netflix.discovery.converters.XmlXStream;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.EurekaServerContextHolder;
import com.netflix.eureka.V1AwareInstanceInfoConverter;
import com.netflix.eureka.aws.AwsBinder;
import com.netflix.eureka.aws.AwsBinderDelegate;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import com.netflix.eureka.util.EurekaMonitors;
import com.thoughtworks.xstream.XStream;

import lombok.extern.apachecommons.CommonsLog;

/**
 * @author Spencer Gibb
 */
@CommonsLog
public class EurekaServerBootstrap {
	private static final String TEST = "test";

	private static final String ARCHAIUS_DEPLOYMENT_ENVIRONMENT = "archaius.deployment.environment";

	private static final String EUREKA_ENVIRONMENT = "eureka.environment";

	private static final String DEFAULT = "default";

	private static final String ARCHAIUS_DEPLOYMENT_DATACENTER = "archaius.deployment.datacenter";

	private static final String EUREKA_DATACENTER = "eureka.datacenter";

	protected EurekaServerConfig eurekaServerConfig;

	protected ApplicationInfoManager applicationInfoManager;

	protected EurekaClientConfig eurekaClientConfig;

	protected PeerAwareInstanceRegistry registry;

	protected volatile EurekaServerContext serverContext;
	protected volatile AwsBinder awsBinder;

	public EurekaServerBootstrap(ApplicationInfoManager applicationInfoManager,
			EurekaClientConfig eurekaClientConfig, EurekaServerConfig eurekaServerConfig,
			PeerAwareInstanceRegistry registry, EurekaServerContext serverContext) {
		this.applicationInfoManager = applicationInfoManager;
		this.eurekaClientConfig = eurekaClientConfig;
		this.eurekaServerConfig = eurekaServerConfig;
		this.registry = registry;
		this.serverContext = serverContext;
	}

	public void contextInitialized(ServletContext context) {
		try {
			initEurekaEnvironment();
			initEurekaServerContext();

			context.setAttribute(EurekaServerContext.class.getName(), this.serverContext);
		}
		catch (Throwable e) {
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

		}
		catch (Throwable e) {
			log.error("Error shutting down eureka", e);
		}
		log.info("Eureka Service is now shutdown...");
	}

	protected void initEurekaEnvironment() throws Exception {
		log.info("Setting the eureka configuration..");

		String dataCenter = ConfigurationManager.getConfigInstance()
				.getString(EUREKA_DATACENTER);
		if (dataCenter == null) {
			log.info(
					"Eureka data center value eureka.datacenter is not set, defaulting to default");
			ConfigurationManager.getConfigInstance()
					.setProperty(ARCHAIUS_DEPLOYMENT_DATACENTER, DEFAULT);
		}
		else {
			ConfigurationManager.getConfigInstance()
					.setProperty(ARCHAIUS_DEPLOYMENT_DATACENTER, dataCenter);
		}
		String environment = ConfigurationManager.getConfigInstance()
				.getString(EUREKA_ENVIRONMENT);
		if (environment == null) {
			ConfigurationManager.getConfigInstance()
					.setProperty(ARCHAIUS_DEPLOYMENT_ENVIRONMENT, TEST);
			log.info(
					"Eureka environment value eureka.environment is not set, defaulting to test");
		}
		else {
			ConfigurationManager.getConfigInstance()
					.setProperty(ARCHAIUS_DEPLOYMENT_ENVIRONMENT, environment);
		}
	}

	protected void initEurekaServerContext() throws Exception {
		// For backward compatibility
		JsonXStream.getInstance().registerConverter(new V1AwareInstanceInfoConverter(),
				XStream.PRIORITY_VERY_HIGH);
		XmlXStream.getInstance().registerConverter(new V1AwareInstanceInfoConverter(),
				XStream.PRIORITY_VERY_HIGH);

		if (isAws(this.applicationInfoManager.getInfo())) {
			this.awsBinder = new AwsBinderDelegate(this.eurekaServerConfig,
					this.eurekaClientConfig, this.registry, this.applicationInfoManager);
			this.awsBinder.start();
		}

		EurekaServerContextHolder.initialize(this.serverContext);

		log.info("Initialized server context");

		// Copy registry from neighboring eureka node
		int registryCount = this.registry.syncUp();
		this.registry.openForTraffic(this.applicationInfoManager, registryCount);

		// Register all monitoring statistics.
		EurekaMonitors.registerAllStats();
	}

	/**
	 * Server context shutdown hook. Override for custom logic
	 */
	protected void destroyEurekaServerContext() throws Exception {
		EurekaMonitors.shutdown();
		if (this.awsBinder != null) {
			this.awsBinder.shutdown();
		}
		if (this.serverContext != null) {
			this.serverContext.shutdown();
		}
	}

	/**
	 * Users can override to clean up the environment themselves.
	 */
	protected void destroyEurekaEnvironment() throws Exception {
	}

	protected boolean isAws(InstanceInfo selfInstanceInfo) {
		boolean result = DataCenterInfo.Name.Amazon == selfInstanceInfo
				.getDataCenterInfo().getName();
		log.info("isAws returned " + result);
		return result;
	}

}
