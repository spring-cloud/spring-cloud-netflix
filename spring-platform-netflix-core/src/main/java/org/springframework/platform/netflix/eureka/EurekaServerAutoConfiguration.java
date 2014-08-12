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

import com.netflix.eureka.PeerAwareInstanceRegistry;
import com.netflix.eureka.lease.LeaseManager;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.platform.netflix.eureka.advice.PiggybackMethodInterceptor;
import org.springframework.platform.netflix.eureka.event.EurekaRegistryAvailableEvent;
import org.springframework.platform.netflix.eureka.event.EurekaServerStartedEvent;
import org.springframework.platform.netflix.eureka.event.LeaseManagerMessageBroker;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.ServletContextAware;

import com.netflix.blitz4j.LoggingConfiguration;
import com.netflix.eureka.EurekaBootStrap;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.EurekaServerConfigurationManager;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * @author Dave Syer
 *
 */
@Configuration
@EnableConfigurationProperties(EurekaServerConfigBean.class)
@ConditionalOnClass(EurekaServerConfig.class)
@ConditionalOnExpression("${eureka.server.enabled:true}")
@EnableEurekaClient
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
						//PeerAwareInstanceRegistry.getInstance();
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

    @Configuration
    @ConditionalOnClass(PeerAwareInstanceRegistry.class)
    protected static class Initializer implements
            ApplicationListener<EurekaRegistryAvailableEvent> {

        @Autowired
        private ApplicationContext applicationContext;

        @Bean
        public LeaseManagerMessageBroker leaseManagerMessageBroker() {
            return new LeaseManagerMessageBroker();
        }

        @Override
        public void onApplicationEvent(EurekaRegistryAvailableEvent event) {
            //wrap the instance registry...
            ProxyFactory factory = new ProxyFactory(PeerAwareInstanceRegistry.getInstance());
            //...with the LeaseManagerMessageBroker
            factory.addAdvice(new PiggybackMethodInterceptor(leaseManagerMessageBroker(), LeaseManager.class));
            factory.setProxyTargetClass(true);

            //Now replace the PeerAwareInstanceRegistry with our wrapped version
            Field field = ReflectionUtils.findField(PeerAwareInstanceRegistry.class, "instance");
            try {
                // Awful ugly hack to work around lack of DI in eureka
                field.setAccessible(true);
                Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                ReflectionUtils.setField(field, null, factory.getProxy());
            }
            catch (Exception e) {
                throw new IllegalStateException("Cannot modify instance registry", e);
            }
        }

    }
}
