/*
 * Copyright 2013-2017 the original author or authors.
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
package org.springframework.cloud.netflix.ribbon.okhttp;

import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicyFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration;
import org.springframework.cloud.commons.httpclient.HttpClientConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonClientConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancedRetryPolicyFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;

/**
 * @author Ryan Baxter
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(value = { "ribbon.okhttp.enabled: true", "ribbon.httpclient.enabled: false" })
@ContextConfiguration(classes = { RibbonAutoConfiguration.class,
		HttpClientConfiguration.class, RibbonClientConfiguration.class,
		LoadBalancerAutoConfiguration.class })
public class SpringRetryEnabledOkHttpClientTests implements ApplicationContextAware {

	private ApplicationContext context;

	@Test
	public void testLoadBalancedRetryFactoryBean() throws Exception {
		Map<String, LoadBalancedRetryPolicyFactory> factories = context
				.getBeansOfType(LoadBalancedRetryPolicyFactory.class);
		assertThat(factories.values(), hasSize(1));
		assertThat(factories.values().toArray()[0],
				instanceOf(RibbonLoadBalancedRetryPolicyFactory.class));
		Map<String, OkHttpLoadBalancingClient> clients = context
				.getBeansOfType(OkHttpLoadBalancingClient.class);
		assertThat(clients.values(), hasSize(1));
		assertThat(clients.values().toArray()[0],
				instanceOf(RetryableOkHttpLoadBalancingClient.class));
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}
}
