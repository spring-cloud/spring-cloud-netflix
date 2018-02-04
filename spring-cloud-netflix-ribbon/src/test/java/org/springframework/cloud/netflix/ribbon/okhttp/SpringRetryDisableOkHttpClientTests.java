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
package org.springframework.cloud.netflix.ribbon.okhttp;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicyFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration;
import org.springframework.cloud.commons.httpclient.HttpClientConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonClientConfiguration;
import org.springframework.cloud.test.ClassPathExclusions;
import org.springframework.cloud.test.ModifiedClassPathRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;

/**
 * @author Ryan Baxter
 * @author Biju Kunjummen
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions({"spring-retry-*.jar", "spring-boot-starter-aop-*.jar"})
public class SpringRetryDisableOkHttpClientTests {

	@Test
	public void testLoadBalancedRetryFactoryBean() {
		new ApplicationContextRunner()
			.withPropertyValues("ribbon.okhttp.enabled=true")
			.withConfiguration(AutoConfigurations.of(RibbonAutoConfiguration.class,
				LoadBalancerAutoConfiguration.class,
				HttpClientConfiguration.class, RibbonClientConfiguration.class))
			.withUserConfiguration(
				OkHttpLoadBalancingClientTests.OkHttpClientConfiguration.class)
			.run(context -> {
				Map<String, LoadBalancedRetryPolicyFactory> factories = context.getBeansOfType(LoadBalancedRetryPolicyFactory.class);
				assertThat(factories.values(), hasSize(1));
				assertThat(factories.values().toArray()[0], instanceOf(LoadBalancedRetryPolicyFactory.NeverRetryFactory.class));
				Map<String, OkHttpLoadBalancingClient> clients = context.getBeansOfType(OkHttpLoadBalancingClient.class);
				assertThat(clients.values(), hasSize(1));
				assertThat(clients.values().toArray()[0], instanceOf(OkHttpLoadBalancingClient.class));
			});
	}
}
