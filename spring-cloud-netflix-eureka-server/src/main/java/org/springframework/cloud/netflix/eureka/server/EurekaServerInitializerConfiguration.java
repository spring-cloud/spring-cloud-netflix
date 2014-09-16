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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

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
import org.springframework.cloud.netflix.eureka.EurekaServerConfigBean;
import org.springframework.cloud.netflix.eureka.advice.PiggybackMethodInterceptor;
import org.springframework.cloud.netflix.eureka.event.EurekaRegistryAvailableEvent;
import org.springframework.cloud.netflix.eureka.event.EurekaServerStartedEvent;
import org.springframework.cloud.netflix.eureka.event.LeaseManagerMessageBroker;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.ServletContextAware;

import com.netflix.blitz4j.LoggingConfiguration;
import com.netflix.eureka.EurekaBootStrap;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.EurekaServerConfigurationManager;
import com.netflix.eureka.PeerAwareInstanceRegistry;
import com.netflix.eureka.lease.LeaseManager;

/**
 * @author Dave Syer
 *
 */
@Configuration
@EnableConfigurationProperties(EurekaServerConfigBean.class)
public class EurekaServerInitializerConfiguration implements ServletContextAware,
		SmartLifecycle, Ordered {

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

	@Override
	public void start() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					new EurekaBootStrap() {
						@Override
						protected void initEurekaEnvironment() {
							LoggingConfiguration.getInstance().configure();
							EurekaServerConfigurationManager.getInstance()
									.setConfiguration(eurekaServerConfig);
							// PeerAwareInstanceRegistry.getInstance();
							applicationContext
									.publishEvent(new EurekaRegistryAvailableEvent(
											eurekaServerConfig));
						}
					}.contextInitialized(new ServletContextEvent(servletContext));
					running = true;
					applicationContext.publishEvent(new EurekaServerStartedEvent(
							eurekaServerConfig));
				}
				catch (Exception e) {
					// Help!
					logger.error("Could not initialize Eureka servlet context", e);
				}
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

	@Configuration
	@ConditionalOnClass(PeerAwareInstanceRegistry.class)
	protected static class RegistryInstanceProxyInitializer implements
			SmartApplicationListener {

		@Autowired
		private ApplicationContext applicationContext;

		private PeerAwareInstanceRegistry instance;

		@Bean
		public LeaseManagerMessageBroker leaseManagerMessageBroker() {
			return new LeaseManagerMessageBroker();
		}

		@Override
		public void onApplicationEvent(ApplicationEvent event) {
			if (instance == null) {
				instance = PeerAwareInstanceRegistry.getInstance();
				// Our instance is the un-proxied version so we can hack it
				expectRegistrations(1);
			}
			if (event instanceof EurekaServerStartedEvent) {
				// Do it again in case this message came in late
				expectRegistrations(1);
			}
			else {
				replaceInstance(getProxyForInstance());
			}
		}

		private void replaceInstance(Object proxy) {
			Field field = ReflectionUtils.findField(PeerAwareInstanceRegistry.class,
					"instance");
			try {
				// Awful ugly hack to work around lack of DI in eureka
				field.setAccessible(true);
				Field modifiersField = Field.class.getDeclaredField("modifiers");
				modifiersField.setAccessible(true);
				modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
				ReflectionUtils.setField(field, null, proxy);
			}
			catch (Exception e) {
				throw new IllegalStateException("Cannot modify instance registry", e);
			}
		}

		private Object getProxyForInstance() {
			// Wrap the instance registry...
			ProxyFactory factory = new ProxyFactory(instance);
			// ...with the LeaseManagerMessageBroker
			factory.addAdvice(new PiggybackMethodInterceptor(leaseManagerMessageBroker(),
					LeaseManager.class));
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
			Field field = ReflectionUtils.findField(PeerAwareInstanceRegistry.class,
					"expectedNumberOfRenewsPerMin");
			try {
				// Awful ugly hack to work around lack of DI in eureka
				field.setAccessible(true);
				int value = (int) ReflectionUtils.getField(field, instance);
				if (value == 0 && count > 0) {
					ReflectionUtils.setField(field, instance, count);
				}
			}
			catch (Exception e) {
				throw new IllegalStateException(
						"Cannot modify instance registry expected renews", e);
			}
		}

		@Override
		public int getOrder() {
			return 0;
		}

		@Override
		public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
			return eventType.isAssignableFrom(EurekaServerStartedEvent.class)
					|| eventType.isAssignableFrom(EurekaRegistryAvailableEvent.class);
		}

		@Override
		public boolean supportsSourceType(Class<?> sourceType) {
			return true;
		}

	}

	/**
	 * Additional aspect for intercepting method invocations on PeerAwareInstanceRegistry.
	 * If {@link PeerAwareInstanceRegistry#openForTraffic(int)} is called with a zero
	 * argument, it means that leases are not automatically cancelled if the instance
	 * hasn't sent any renewals recently. This happens for a standalone server. It seems
	 * like a bad default, so we set it to the smallest non-zero value we can, so that any
	 * instances that subsequently register can bump up the threshold.
	 * 
	 * @author Dave Syer
	 *
	 */
	protected static class TrafficOpener implements MethodInterceptor {

		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {
			if ("openForTraffic".equals(invocation.getMethod().getName())) {
				int count = (int) invocation.getArguments()[0];
				if (count == 0) {
					ReflectionUtils.invokeMethod(invocation.getMethod(),
							invocation.getThis(), 1);
					return null;
				}
			}
			return invocation.proceed();
		}
	}
}
