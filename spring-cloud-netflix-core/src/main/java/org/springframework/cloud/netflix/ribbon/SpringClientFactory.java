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

import org.springframework.beans.BeanUtils;
import org.springframework.cloud.context.named.NamedContextFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

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
public class SpringClientFactory extends NamedContextFactory<RibbonClientSpecification> {

	static final String NAMESPACE = "ribbon";

	public SpringClientFactory() {
		super(RibbonClientConfiguration.class, NAMESPACE, "ribbon.client.name");
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

	static <C> C instantiateWithConfig(Class<C> clazz, IClientConfig config) {
		return instantiateWithConfig(null, clazz, config);
	}

	static <C> C instantiateWithConfig(AnnotationConfigApplicationContext context,
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
		if (context != null) {
			context.getAutowireCapableBeanFactory().autowireBean(result);
		}
		return result;
	}

	public <C> C getInstance(String name, Class<C> type) {
		C instance = super.getInstance(name, type);
		if (instance != null) {
			return instance;
		}
		IClientConfig config = getInstance(name, IClientConfig.class);
		return instantiateWithConfig(getContext(name), type, config);
	}

}

