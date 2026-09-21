/*
 * Copyright 2017-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.eureka.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import jakarta.annotation.Priority;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Supplies HTTP client builders for Eureka registry transport, excluding
 * {@link LoadBalanced @LoadBalanced} beans so registry hosts are not resolved as service
 * ids.
 *
 * @author arimu1
 */
final class EurekaClientBuilderSuppliers {

	private EurekaClientBuilderSuppliers() {
	}

	static Supplier<RestClient.Builder> restClientBuilder(ConfigurableListableBeanFactory beanFactory) {
		RestClient.Builder builder = resolveNonLoadBalancedBuilder(beanFactory, RestClient.Builder.class,
				RestClient::builder);
		return () -> builder;
	}

	static Supplier<WebClient.Builder> webClientBuilder(ConfigurableListableBeanFactory beanFactory) {
		WebClient.Builder builder = resolveNonLoadBalancedBuilder(beanFactory, WebClient.Builder.class,
				WebClient::builder);
		return () -> builder;
	}

	private static <T> T resolveNonLoadBalancedBuilder(ConfigurableListableBeanFactory beanFactory, Class<T> type,
			Supplier<T> fallback) {
		List<String> candidates = new ArrayList<>();
		for (String name : beanFactory.getBeanNamesForType(type)) {
			if (beanFactory.findAnnotationOnBean(name, LoadBalanced.class) == null) {
				candidates.add(name);
			}
		}
		if (candidates.isEmpty()) {
			return fallback.get();
		}
		String beanName = selectHighestPriorityBeanName(beanFactory, candidates);
		return beanFactory.getBean(beanName, type);
	}

	private static String selectHighestPriorityBeanName(ConfigurableListableBeanFactory beanFactory,
			List<String> candidates) {
		if (candidates.size() == 1) {
			return candidates.get(0);
		}
		candidates.sort(
				Comparator.comparingInt((String name) -> orderFor(beanFactory, name)).thenComparing(String::compareTo));
		return candidates.get(0);
	}

	private static int orderFor(ConfigurableListableBeanFactory beanFactory, String beanName) {
		if (beanFactory instanceof DefaultListableBeanFactory defaultListableBeanFactory) {
			return defaultListableBeanFactory.getOrder(beanName);
		}
		Order order = beanFactory.findAnnotationOnBean(beanName, Order.class);
		if (order != null) {
			return order.value();
		}
		Priority priority = beanFactory.findAnnotationOnBean(beanName, Priority.class);
		if (priority != null) {
			return priority.value();
		}
		Object bean = beanFactory.getBean(beanName);
		if (bean instanceof Ordered ordered) {
			return ordered.getOrder();
		}
		return Ordered.LOWEST_PRECEDENCE;
	}

}
