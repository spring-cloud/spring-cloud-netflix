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
package org.springframework.cloud.netflix.config;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.DiscoveryClient;

/**
 * @author Dave Syer
 *
 */
public class DiscoveryClientConfigServiceBootstrapConfigurationTests {

	private AnnotationConfigApplicationContext context;

	private DiscoveryClient client = Mockito.mock(DiscoveryClient.class);

	private InstanceInfo info = InstanceInfo.Builder.newBuilder().setAppName("app")
			.setHostName("foo").setHomePageUrl("/", null).build();

	@After
	public void close() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	public void offByDefault() throws Exception {
		context = new AnnotationConfigApplicationContext(
				DiscoveryClientConfigServiceBootstrapConfiguration.class);
		assertEquals(0, context.getBeanNamesForType(DiscoveryClient.class).length);
		assertEquals(
				0,
				context.getBeanNamesForType(DiscoveryClientConfigServiceBootstrapConfiguration.class).length);
	}

	@Test
	public void onWhenRequested() throws Exception {
		Mockito.when(client.getNextServerFromEureka("CONFIGSERVER", false)).thenReturn(
				info);
		setup("spring.cloud.config.discovery.enabled=true");
		assertEquals(
				1,
				context.getBeanNamesForType(DiscoveryClientConfigServiceBootstrapConfiguration.class).length);
		Mockito.verify(client).getNextServerFromEureka("CONFIGSERVER", false);
		ConfigServicePropertySourceLocator locator = context
				.getBean(ConfigServicePropertySourceLocator.class);
		assertEquals("http://foo:7001/", locator.getUri());
	}

	@Test
	public void setsPasssword() throws Exception {
		info.getMetadata().put("password", "bar");
		Mockito.when(client.getNextServerFromEureka("CONFIGSERVER", false)).thenReturn(
				info);
		setup("spring.cloud.config.discovery.enabled=true");
		ConfigServicePropertySourceLocator locator = context
				.getBean(ConfigServicePropertySourceLocator.class);
		assertEquals("http://foo:7001/", locator.getUri());
		assertEquals("bar", locator.getPassword());
		assertEquals("user", locator.getUsername());
	}

	private void setup(String... env) {
		context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(context, env);
		context.getDefaultListableBeanFactory().registerSingleton("mockDiscoveryClient",
				client);
		context.register(PropertyPlaceholderAutoConfiguration.class,
				DiscoveryClientConfigServiceBootstrapConfiguration.class,
				ConfigServicePropertySourceLocator.class);
		context.refresh();		
	}

}
