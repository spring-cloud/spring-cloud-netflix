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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;

import javax.annotation.PostConstruct;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClientConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;

/**
 * @author Dave Syer
 */
public class DiscoveryClientConfigServiceAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	private EurekaClient client = Mockito.mock(EurekaClient.class);

	private InstanceInfo info = InstanceInfo.Builder.newBuilder().setAppName("app")
			.setHostName("foo").setHomePageUrl("/", null).build();

	@After
	public void close() {
		if (this.context != null) {
			if (this.context.getParent() != null) {
				((AnnotationConfigApplicationContext) this.context.getParent()).close();
			}
			this.context.close();
		}
	}

	@Test
	public void onWhenRequested() throws Exception {
		given(this.client.getNextServerFromEureka("CONFIGSERVER", false))
				.willReturn(this.info);
		setup("spring.cloud.config.discovery.enabled=true");
		assertEquals(1, this.context.getBeanNamesForType(
				DiscoveryClientConfigServiceAutoConfiguration.class).length);
		Mockito.verify(this.client, times(2)).getNextServerFromEureka("CONFIGSERVER",
				false);
		Mockito.verify(this.client, times(1)).shutdown();
		ConfigClientProperties locator = this.context
				.getBean(ConfigClientProperties.class);
		assertEquals("http://foo:7001/", locator.getRawUri());
		ApplicationInfoManager infoManager = this.context
				.getBean(ApplicationInfoManager.class);
		assertEquals("bar", infoManager.getInfo().getMetadata().get("foo"));
	}

	private void setup(String... env) {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(parent, env);
		parent.getDefaultListableBeanFactory().registerSingleton("eurekaClient",
				this.client);
		parent.register(PropertyPlaceholderAutoConfiguration.class,
				DiscoveryClientConfigServiceBootstrapConfiguration.class,
				EnvironmentKnobbler.class, ConfigClientProperties.class);
		parent.refresh();
		this.context = new AnnotationConfigApplicationContext();
		this.context.setParent(parent);
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				DiscoveryClientConfigServiceAutoConfiguration.class,
				EurekaClientAutoConfiguration.class,
				EurekaDiscoveryClientConfiguration.class);
		this.context.refresh();
	}

	@Configuration
	protected static class EnvironmentKnobbler {

		@Autowired
		private ConfigurableEnvironment environment;

		@PostConstruct
		public void init() {
			EnvironmentTestUtils.addEnvironment(this.environment,
					"eureka.instance.metadataMap.foo:bar");
		}

	}

}
