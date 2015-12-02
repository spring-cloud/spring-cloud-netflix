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

package org.springframework.cloud.netflix.feign;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

/**
 * A factory that creates client, load balancer and client configuration instances. It
 * creates a Spring ApplicationContext per client name, and extracts the beans that it
 * needs from there.
 *
 * @author Spencer Gibb
 * @author Dave Syer
 */
public class FeignClientFactory implements DisposableBean, ApplicationContextAware {

	private Map<String, AnnotationConfigApplicationContext> contexts = new ConcurrentHashMap<>();

	private Map<String, FeignClientSpecification> configurations = new ConcurrentHashMap<>();

	private ApplicationContext parent;

	@Override
	public void setApplicationContext(ApplicationContext parent) throws BeansException {
		this.parent = parent;
	}

	public void setConfigurations(List<FeignClientSpecification> configurations) {
		for (FeignClientSpecification client : configurations) {
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
		for (Entry<String, FeignClientSpecification> entry : this.configurations
				.entrySet()) {
			if (entry.getKey().startsWith("default.")) {
				for (Class<?> configuration : entry.getValue().getConfiguration()) {
					context.register(configuration);
				}
			}
		}
		context.register(PropertyPlaceholderAutoConfiguration.class,
				FeignClientsConfiguration.class);
		context.getEnvironment().getPropertySources()
				.addFirst(new MapPropertySource("feign",
								Collections.<String, Object> singletonMap(
										"feign.client.name", name)));
		if (this.parent != null) {
			// Uses Environment from parent as well as beans
			context.setParent(this.parent);
		}
		context.refresh();
		return context;
	}

	public <C> C getInstance(String name, Class<C> type) {
		AnnotationConfigApplicationContext context = getContext(name);
		if (BeanFactoryUtils.beanNamesForTypeIncludingAncestors(context, type).length > 0) {
			return context.getBean(type);
		}
		return null;
	}


	public <C> Map<String, C> getInstances(String name, Class<C> type) {
		AnnotationConfigApplicationContext context = getContext(name);
		if (BeanFactoryUtils.beanNamesForTypeIncludingAncestors(context, type).length > 0) {
			return context.getBeansOfType(type);
		}
		return null;
	}

}
