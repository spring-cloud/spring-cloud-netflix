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

package org.springframework.cloud.netflix.ribbon;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import com.netflix.client.IClient;
import com.netflix.client.IClientConfigAware;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;

/**
 * A factory that creates client, load balancer and client configuration instances. It
 * creates a Spring ApplicationContext per client name, and extracts the beans that it
 * needs from there.
 *
 * @author Spencer Gibb
 * @author Dave Syer
 */
public class SpringClientFactory implements DisposableBean, ApplicationContextAware {

	private Map<String, AnnotationConfigApplicationContext> contexts = new ConcurrentHashMap<>();

	private Map<String, RibbonClientSpecification> configurations = new ConcurrentHashMap<>();

	private ApplicationContext parent;

	@Override
	public void setApplicationContext(ApplicationContext parent) throws BeansException {
		this.parent = parent;
	}

	public void setConfigurations(List<RibbonClientSpecification> configurations) {
		for (RibbonClientSpecification client : configurations) {
			this.configurations.put(client.getName(), client);
		}
	}

	@Override
	public void destroy() {
		Collection<AnnotationConfigApplicationContext> values = this.contexts.values();
		this.contexts.clear();
		for (AnnotationConfigApplicationContext context : values) {
			context.close();
		}
	}

	/**
	 * Get the rest client associated with the name.
	 * @throws RuntimeException if any error occurs
	 */
	public <C extends IClient<?, ?>> C getClient(String name, Class<C> clientClass) {
		return getInstance(name, clientClass);
	}

	/**
	 * Get the load balancer associated with the name.
	 * @throws RuntimeException if any error occurs
	 */
	public ILoadBalancer getLoadBalancer(String name) {
		return getInstance(name, ILoadBalancer.class);
	}

	/**
	 * Get the client config associated with the name.
	 * @throws RuntimeException if any error occurs
	 */
	public IClientConfig getClientConfig(String name) {
		return getInstance(name, IClientConfig.class);
	}

	/**
	 * Get the load balancer context associated with the name.
	 * @throws RuntimeException if any error occurs
	 */
	public RibbonLoadBalancerContext getLoadBalancerContext(String serviceId) {
		return getInstance(serviceId, RibbonLoadBalancerContext.class);
	}

	private AnnotationConfigApplicationContext getContext(String name) {
		if (!this.contexts.containsKey(name)) {
			synchronized (this.contexts) {
				if (!this.contexts.containsKey(name)) {
					this.contexts.put(name, createContext(name));
				}
			}
		}
		return this.contexts.get(name);
	}

	private AnnotationConfigApplicationContext createContext(String name) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		if (this.configurations.containsKey(name)) {
			for (Class<?> configuration : this.configurations.get(name)
					.getConfiguration()) {
				context.register(configuration);
			}
		}
		for (Entry<String, RibbonClientSpecification> entry : this.configurations
				.entrySet()) {
			if (entry.getKey().startsWith("default.")) {
				for (Class<?> configuration : entry.getValue().getConfiguration()) {
					context.register(configuration);
				}
			}
		}
		context.register(PropertyPlaceholderAutoConfiguration.class,
				RibbonClientConfiguration.class);
		context.getEnvironment()
				.getPropertySources()
				.addFirst(
						new MapPropertySource("ribbon",
								Collections.<String, Object> singletonMap(
										"ribbon.client.name", name)));
		if (this.parent != null) {
			// Uses Environment from parent as well as beans
			context.setParent(this.parent);
		}
		context.refresh();
		return context;
	}

	private <C> C instantiateWithConfig(AnnotationConfigApplicationContext context,
			Class<C> clazz, IClientConfig config) {
		C result = null;
		if (IClientConfigAware.class.isAssignableFrom(clazz)) {
			IClientConfigAware obj = (IClientConfigAware) BeanUtils.instantiate(clazz);
			obj.initWithNiwsConfig(config);
			@SuppressWarnings("unchecked")
			C value = (C) obj;
			result = value;
		}
		else {
			try {
				if (clazz.getConstructor(IClientConfig.class) != null) {
					result = clazz.getConstructor(IClientConfig.class)
							.newInstance(config);
				}
				else {
					result = BeanUtils.instantiate(clazz);
				}
			}
			catch (Throwable ex) {
				// NOPMD
			}
		}
		context.getAutowireCapableBeanFactory().autowireBean(result);
		return result;
	}

	public <C> C getInstance(String name, Class<C> type) {
		AnnotationConfigApplicationContext context = getContext(name);
		if (BeanFactoryUtils.beanNamesForTypeIncludingAncestors(context, type).length > 0) {
			return context.getBean(type);
		}
		IClientConfig config = getInstance(name, IClientConfig.class);
		return instantiateWithConfig(context, type, config);
	}

}
