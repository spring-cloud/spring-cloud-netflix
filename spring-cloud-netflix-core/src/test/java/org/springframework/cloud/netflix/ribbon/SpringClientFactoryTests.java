/*
 * Copyright 2013-2014 the original author or authors.
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

import org.junit.Test;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.netflix.client.DefaultLoadBalancerRetryHandler;

import static org.junit.Assert.assertEquals;

/**
 * @author Dave Syer
 *
 */
public class SpringClientFactoryTests {

	private SpringClientFactory factory = new SpringClientFactory();

	@Test
	public void testConfigureRetry() {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext(
				ArchaiusAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(parent, "foo.ribbon.MaxAutoRetries:2");
		this.factory.setApplicationContext(parent);
		DefaultLoadBalancerRetryHandler retryHandler = (DefaultLoadBalancerRetryHandler) this.factory
				.getLoadBalancerContext("foo").getRetryHandler();
		assertEquals(2, retryHandler.getMaxRetriesOnSameServer());
		parent.close();
		this.factory.destroy();
	}

}
