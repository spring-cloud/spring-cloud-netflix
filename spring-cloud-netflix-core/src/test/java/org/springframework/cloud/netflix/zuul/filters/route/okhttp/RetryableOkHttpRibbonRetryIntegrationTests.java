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

package org.springframework.cloud.netflix.zuul.filters.route.okhttp;

import com.netflix.client.RetryHandler;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicyFactory;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.cloud.netflix.ribbon.okhttp.RetryableOkHttpLoadBalancingClient;
import org.springframework.cloud.netflix.zuul.filters.route.support.RibbonRetryIntegrationTestBase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Ryan Baxter
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {RetryableOkHttpRibbonRetryIntegrationTests.RetryableTestConfig.class, RetryableOkHttpRibbonRetryIntegrationTests.RibbonClientsConfiguration.class},
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		value = {
				"zuul.retryable: false", /* Disable retry by default, have each route enable it */
				"ribbon.okhttp.enabled: true",
				"hystrix.command.default.execution.timeout.enabled: false", /* Disable hystrix so its timeout doesnt get in the way */
				"ribbon.ReadTimeout: 1000", /* Make sure ribbon will timeout before the thread is done sleeping */
				"zuul.routes.retryable: /retryable/**",
				"zuul.routes.retryable.retryable: true",
				"retryable.ribbon.OkToRetryOnAllOperations: true",
				"retryable.ribbon.MaxAutoRetries: 1",
				"retryable.ribbon.MaxAutoRetriesNextServer: 1",
				"zuul.routes.getretryable: /getretryable/**",
				"zuul.routes.getretryable.retryable: true",
				"getretryable.ribbon.MaxAutoRetries: 1",
				"getretryable.ribbon.MaxAutoRetriesNextServer: 1",
				"zuul.routes.disableretry: /disableretry/**",
				"zuul.routes.disableretry.retryable: false", /* This will override the global */
				"disableretry.ribbon.MaxAutoRetries: 1",
				"disableretry.ribbon.MaxAutoRetriesNextServer: 1",
				"zuul.routes.globalretrydisabled: /globalretrydisabled/**",
				"globalretrydisabled.ribbon.MaxAutoRetries: 1",
				"globalretrydisabled.ribbon.MaxAutoRetriesNextServer: 1",
				"hystrix.command.stopretry.execution.timeout.enabled: true",
				"hystrix.command.stopretry.execution.isolation.thread.timeoutInMilliseconds: 350",
				"zuul.routes.stopretry.retryable: true",
				"stopretry.ribbon.ReadTimeout: 100",
				"stopretry.ribbon.OkToRetryOnAllOperations: true",
				"stopretry.ribbon.MaxAutoRetries: 10",
				"stopretry.ribbon.MaxAutoRetriesNextServer: 0"
})
@DirtiesContext
public class RetryableOkHttpRibbonRetryIntegrationTests extends RibbonRetryIntegrationTestBase {

	@Configuration
	@RibbonClients({
			@RibbonClient(name = "retryable", configuration = RibbonClientConfiguration.class),
			@RibbonClient(name = "disableretry", configuration = RibbonClientConfiguration.class),
			@RibbonClient(name = "globalretrydisabled", configuration = RibbonClientConfiguration.class),
			@RibbonClient(name = "getretryable", configuration = RibbonClientConfiguration.class),
			@RibbonClient(name = "stopretry", configuration = RibbonClientConfiguration.class)})
	public static class RibbonClientsConfiguration {
	}

	@Configuration
	public static class RibbonClientConfiguration {

		@Value("${local.server.port}")
		private int port;

		@Bean
		public ServerList<Server> ribbonServerList() {
			return new StaticServerList<>(new Server("localhost", this.port));
		}

		@Bean
		public RetryableOkHttpLoadBalancingClient okHttpLoadBalancingClient(IClientConfig config,
																			ServerIntrospector serverIntrospector,
																			ILoadBalancer loadBalancer,
																			RetryHandler retryHandler,
																			LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory) {
			RetryableOkHttpLoadBalancingClient client = new RetryableOkHttpLoadBalancingClient(config,
					serverIntrospector, loadBalancedRetryPolicyFactory);
			client.setLoadBalancer(loadBalancer);
			client.setRetryHandler(retryHandler);
			return client;
		}
	}
}
