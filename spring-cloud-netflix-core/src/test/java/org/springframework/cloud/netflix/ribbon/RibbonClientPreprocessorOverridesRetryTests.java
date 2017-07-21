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

package org.springframework.cloud.netflix.ribbon;

import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import com.netflix.client.DefaultLoadBalancerRetryHandler;
import com.netflix.client.RetryHandler;
import com.netflix.client.config.IClientConfig;

/**
 * @author Tyler Van Gorder
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = RibbonClientPreprocessorOverridesRetryTests.TestConfiguration.class, value = {
		"customRetry.ribbon.MaxAutoRetries=0",
		"customRetry.ribbon.MaxAutoRetriesNextServer=1",
		"customRetry.ribbon.OkToRetryOnAllOperations=true" })
@DirtiesContext
public class RibbonClientPreprocessorOverridesRetryTests {

	@Autowired
	private SpringClientFactory factory;

	@Test
	public void customRetryIsConfigured() throws Exception {
		RibbonLoadBalancerContext context = (RibbonLoadBalancerContext) this.factory
				.getLoadBalancerContext("customRetry");
		Assert.isInstanceOf(RetryRibbonConfiguration.CustomRetryHandler.class,
				context.getRetryHandler());
		Assert.isTrue(context.getRetryHandler().getMaxRetriesOnSameServer() == 0);
		Assert.isTrue(context.getRetryHandler().getMaxRetriesOnNextServer() == 1);
		Assert.isTrue(context.getRetryHandler()
				.isCircuitTrippingException(new UnknownHostException("Unknown Host")));
	}

	@Configuration
	@RibbonClient(name = "customRetry", configuration = RetryRibbonConfiguration.class)
	@Import({ PropertyPlaceholderAutoConfiguration.class, ArchaiusAutoConfiguration.class,
			RibbonAutoConfiguration.class })
	protected static class TestConfiguration {
	}

}

@Configuration
class RetryRibbonConfiguration {
	@Bean
	public RetryHandler retryHandler(IClientConfig config) {
		return new CustomRetryHandler(config);
	}

	class CustomRetryHandler extends DefaultLoadBalancerRetryHandler {

		@SuppressWarnings("unchecked")
		private List<Class<? extends Throwable>> retriable = new ArrayList() {
			{
				add(UnknownHostException.class);
				add(ConnectException.class);
				add(SocketTimeoutException.class);
			}
		};

		@SuppressWarnings("unchecked")
		private List<Class<? extends Throwable>> circuitRelated = new ArrayList() {
			{
				add(UnknownHostException.class);
				add(SocketException.class);
				add(SocketTimeoutException.class);
			}
		};

		CustomRetryHandler(IClientConfig config) {
			super(config);
		}

		@Override
		protected List<Class<? extends Throwable>> getRetriableExceptions() {
			return retriable;
		}

		@Override
		protected List<Class<? extends Throwable>> getCircuitRelatedExceptions() {
			return circuitRelated;
		}

	}
}
