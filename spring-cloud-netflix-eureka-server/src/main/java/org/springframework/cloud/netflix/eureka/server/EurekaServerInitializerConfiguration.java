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

package org.springframework.cloud.netflix.eureka.server;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.logging.log4j.Log4JLoggingSystem;
import org.springframework.cloud.netflix.eureka.DataCenterAwareJacksonCodec;
import org.springframework.cloud.netflix.eureka.DataCenterAwareMarshallingStrategy;
import org.springframework.cloud.netflix.eureka.EurekaServerConfigBean;
import org.springframework.cloud.netflix.eureka.server.advice.LeaseManagerLite;
import org.springframework.cloud.netflix.eureka.server.advice.PiggybackMethodInterceptor;
import org.springframework.cloud.netflix.eureka.server.event.EurekaRegistryAvailableEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaServerStartedEvent;
import org.springframework.cloud.netflix.eureka.server.event.LeaseManagerMessageBroker;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.ServletContextAware;

import com.netflix.blitz4j.DefaultBlitz4jConfig;
import com.netflix.blitz4j.LoggingConfiguration;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.converters.JsonXStream;
import com.netflix.discovery.converters.XmlXStream;
import com.netflix.eureka.AbstractInstanceRegistry;
import com.netflix.eureka.EurekaBootStrap;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.EurekaServerConfigurationManager;
import com.netflix.eureka.PeerAwareInstanceRegistry;
import com.netflix.eureka.PeerAwareInstanceRegistryImpl;

/**
 * @author Dave Syer
 */
@Configuration
@EnableConfigurationProperties(EurekaServerConfigBean.class)
public class EurekaServerInitializerConfiguration
		implements ServletContextAware, SmartLifecycle, Ordered {

	private static Log logger = LogFactory
			.getLog(EurekaServerInitializerConfiguration.class);

	@Autowired
	private EurekaServerConfig eurekaServerConfig;

	private ServletContext servletContext;

	@Autowired
	private ApplicationContext applicationContext;

	private boolean running;

	private int order = 1;

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@PostConstruct
	public void initLogging() {

		if (!(LoggingSystem
				.get(ClassUtils.getDefaultClassLoader()) instanceof Log4JLoggingSystem)) {

			LoggingConfiguration off = new LoggingConfiguration() {
				@Override
				public void configure() {
				}
			};
			Field instance = ReflectionUtils.findField(LoggingConfiguration.class,
					"instance");
			ReflectionUtils.makeAccessible(instance);
			ReflectionUtils.setField(instance, null, off);
			Field blitz4j = ReflectionUtils.findField(LoggingConfiguration.class,
					"blitz4jConfig");
			ReflectionUtils.makeAccessible(blitz4j);
			try {
				Properties props = PropertiesLoaderUtils
						.loadAllProperties(new ClassPathResource("log4j.properties",
								Log4JLoggingSystem.class).toString());
				DefaultBlitz4jConfig blit4jConfig = new DefaultBlitz4jConfig(props);
				ReflectionUtils.setField(blitz4j, off, blit4jConfig);
			}
			catch (IOException e) {
			}

		}
	}

	@Override
	public void start() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					new EurekaBootStrap() {
						@Override
						protected void initEurekaEnvironment() {
							try {
								if (System.getProperty("log4j.configuration") == null) {
									System.setProperty("log4j.configuration",
											new ClassPathResource("log4j.properties",
													Log4JLoggingSystem.class).getURL()
															.toString());
								}
							}
							catch (IOException ex) {
								// ignore
							}
							LoggingConfiguration.getInstance().configure();
							EurekaServerConfigurationManager.getInstance()
									.setConfiguration(
											EurekaServerInitializerConfiguration.this.eurekaServerConfig);
							XmlXStream.getInstance().setMarshallingStrategy(
									new DataCenterAwareMarshallingStrategy());
							JsonXStream.getInstance().setMarshallingStrategy(
									new DataCenterAwareMarshallingStrategy());
							DataCenterAwareJacksonCodec.init();
							EurekaServerInitializerConfiguration.this.applicationContext
									.publishEvent(new EurekaRegistryAvailableEvent(
											EurekaServerInitializerConfiguration.this.eurekaServerConfig));
						}
					}.contextInitialized(new ServletContextEvent(
							EurekaServerInitializerConfiguration.this.servletContext));
					logger.info("Started Eureka Server");
					EurekaServerInitializerConfiguration.this.running = true;
					EurekaServerInitializerConfiguration.this.applicationContext
							.publishEvent(new EurekaServerStartedEvent(
									EurekaServerInitializerConfiguration.this.eurekaServerConfig));
				}
				catch (Exception ex) {
					// Help!
					logger.error("Could not initialize Eureka servlet context", ex);
				}
			}
		}).start();
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
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public void stop(Runnable callback) {
		callback.run();
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Configuration
	@ConditionalOnClass(PeerAwareInstanceRegistry.class)
	protected static class RegistryInstanceProxyInitializer
			implements ApplicationListener<EurekaRegistryAvailableEvent> {

		@Autowired(required = false)
		private EurekaClient client;

		private PeerAwareInstanceRegistryImpl instance;

		@Bean
		public LeaseManagerMessageBroker leaseManagerMessageBroker() {
			return new LeaseManagerMessageBroker();
		}

		@Override
		public void onApplicationEvent(EurekaRegistryAvailableEvent event) {
			if (this.client != null) {
				this.client.getApplications(); // force initialization
			}
			if (this.instance == null) {
				this.instance = PeerAwareInstanceRegistryImpl.getInstance();
				safeInit();
				replaceInstance(getProxyForInstance());
				expectRegistrations(1);
			}
		}

		private void safeInit() {
			Method method = ReflectionUtils.findMethod(AbstractInstanceRegistry.class,
					"postInit");
			ReflectionUtils.makeAccessible(method);
			ReflectionUtils.invokeMethod(method, this.instance);
		}

		private void replaceInstance(Object proxy) {
			Field field = ReflectionUtils.findField(PeerAwareInstanceRegistryImpl.class,
					"instance");
			try {
				// Awful ugly hack to work around lack of DI in eureka
				field.setAccessible(true);
				Field modifiersField = Field.class.getDeclaredField("modifiers");
				modifiersField.setAccessible(true);
				modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
				ReflectionUtils.setField(field, null, proxy);
			}
			catch (Exception ex) {
				throw new IllegalStateException("Cannot modify instance registry", ex);
			}
		}

		private Object getProxyForInstance() {
			// Wrap the instance registry...
			ProxyFactory factory = new ProxyFactory(this.instance);
			// ...with the LeaseManagerMessageBroker
			factory.addAdvice(new PiggybackMethodInterceptor(leaseManagerMessageBroker(),
					LeaseManagerLite.class));
			factory.addAdvice(new TrafficOpener());
			factory.setProxyTargetClass(true);

			return factory.getProxy();
		}

		private void expectRegistrations(int count) {
			/*
			 * Setting expectedNumberOfRenewsPerMin to non-zero to ensure that even an
			 * isolated server can adjust its eviction policy to the number of
			 * registrations (when it's zero, even a successful registration won't reset
			 * the rate threshold in InstanceRegistry.register()).
			 */
			Field field = ReflectionUtils.findField(AbstractInstanceRegistry.class,
					"expectedNumberOfRenewsPerMin");
			try {
				// Awful ugly hack to work around lack of DI in eureka
				field.setAccessible(true);
				int value = (int) ReflectionUtils.getField(field, this.instance);
				if (value == 0 && count > 0) {
					ReflectionUtils.setField(field, this.instance, count);
				}
			}
			catch (Exception ex) {
				throw new IllegalStateException(
						"Cannot modify instance registry expected renews", ex);
			}
		}

		/**
		 * Additional aspect for intercepting method invocations on
		 * PeerAwareInstanceRegistry. If
		 * {@link PeerAwareInstanceRegistryImpl#openForTraffic(int)} is called with a zero
		 * argument, it means that leases are not automatically cancelled if the instance
		 * hasn't sent any renewals recently. This happens for a standalone server. It
		 * seems like a bad default, so we set it to the smallest non-zero value we can,
		 * so that any instances that subsequently register can bump up the threshold.
		 */
		private class TrafficOpener implements MethodInterceptor {

			@Override
			public Object invoke(MethodInvocation invocation) throws Throwable {
				if ("openForTraffic".equals(invocation.getMethod().getName())) {
					int count = (int) invocation.getArguments()[0];
					ReflectionUtils.invokeMethod(invocation.getMethod(),
							invocation.getThis(), count == 0 ? 1 : count);
					return null;
				}
				return invocation.proceed();
			}

		}

	}

}
