/*
 * Copyright 2013-2018 the original author or authors.
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
 *
 */

package org.springframework.cloud.netflix.ribbon;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicyFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration;
import org.springframework.cloud.netflix.feign.ribbon.CachingSpringLoadBalancerFactory;
import org.springframework.cloud.netflix.feign.ribbon.FeignLoadBalancer;
import org.springframework.cloud.netflix.feign.ribbon.FeignRibbonClientAutoConfiguration;
import org.springframework.cloud.netflix.feign.ribbon.RetryableFeignLoadBalancer;
import org.springframework.cloud.netflix.ribbon.apache.RibbonLoadBalancingHttpClient;
import org.springframework.cloud.test.ClassPathExclusions;
import org.springframework.cloud.test.ModifiedClassPathRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ryan Baxter
 * @author Biju Kunjummen
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions({ "spring-retry-*.jar", "spring-boot-starter-aop-*.jar" })
public class SpringRetryDisabledTests {

	@Test
	public void testLoadBalancedRetryFactoryBean() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(RibbonAutoConfiguration.class,
						LoadBalancerAutoConfiguration.class,
						RibbonClientConfiguration.class,
						FeignRibbonClientAutoConfiguration.class))
				.run(context -> {
					Map<String, LoadBalancedRetryPolicyFactory> factories = context
							.getBeansOfType(LoadBalancedRetryPolicyFactory.class);
					assertThat(factories.values()).hasSize(1);
					assertThat(factories.values().toArray()[0]).isInstanceOf(
							LoadBalancedRetryPolicyFactory.NeverRetryFactory.class);
					Map<String, RibbonLoadBalancingHttpClient> clients = context
							.getBeansOfType(RibbonLoadBalancingHttpClient.class);
					assertThat(clients.values()).hasSize(1);
					assertThat(clients.values().toArray()[0])
							.isInstanceOf(RibbonLoadBalancingHttpClient.class);
					Map<String, CachingSpringLoadBalancerFactory> lbFactorys = context
							.getBeansOfType(CachingSpringLoadBalancerFactory.class);
					assertThat(lbFactorys.values()).hasSize(1);
					FeignLoadBalancer lb = lbFactorys.values().iterator().next()
							.create("foo");
					assertThat(lb).isInstanceOf(FeignLoadBalancer.class);
					assertThat(lb).isNotInstanceOf(RetryableFeignLoadBalancer.class);
				});

	}
}
