/*
 *
 *  * Copyright 2013-2016 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.cloud.netflix.ribbon;

import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicyFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration;
import org.springframework.cloud.commons.httpclient.HttpClientConfiguration;
import org.springframework.cloud.netflix.feign.ribbon.CachingSpringLoadBalancerFactory;
import org.springframework.cloud.netflix.feign.ribbon.FeignLoadBalancer;
import org.springframework.cloud.netflix.feign.ribbon.FeignRibbonClientAutoConfiguration;
import org.springframework.cloud.netflix.feign.ribbon.RetryableFeignLoadBalancer;
import org.springframework.cloud.netflix.ribbon.apache.RetryableRibbonLoadBalancingHttpClient;
import org.springframework.cloud.netflix.ribbon.apache.RibbonLoadBalancingHttpClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

/**
 * @author Ryan Baxter
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {RibbonAutoConfiguration.class, RibbonClientConfiguration.class, LoadBalancerAutoConfiguration.class,
		FeignRibbonClientAutoConfiguration.class, HttpClientConfiguration.class})
public class SpringRetryEnabledTests implements ApplicationContextAware {

	private ApplicationContext context;

	@Test
	public void testLoadBalancedRetryFactoryBean() throws Exception {
		Map<String, LoadBalancedRetryPolicyFactory> factories =  context.getBeansOfType(LoadBalancedRetryPolicyFactory.class);
		assertThat(factories.values(), hasSize(1));
		assertThat(factories.values().toArray()[0], instanceOf(RibbonLoadBalancedRetryPolicyFactory.class));
		Map<String, RibbonLoadBalancingHttpClient> clients =  context.getBeansOfType(RibbonLoadBalancingHttpClient.class);
		assertThat(clients.values(), hasSize(1));
		assertThat(clients.values().toArray()[0], instanceOf(RetryableRibbonLoadBalancingHttpClient.class));
		Map<String, CachingSpringLoadBalancerFactory> lbFactorys =  context.getBeansOfType(CachingSpringLoadBalancerFactory.class);
		assertThat(lbFactorys.values(), Matchers.hasSize(1));
		FeignLoadBalancer lb =lbFactorys.values().iterator().next().create("foo");
		assertThat(lb, instanceOf(RetryableFeignLoadBalancer.class));
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}
}
