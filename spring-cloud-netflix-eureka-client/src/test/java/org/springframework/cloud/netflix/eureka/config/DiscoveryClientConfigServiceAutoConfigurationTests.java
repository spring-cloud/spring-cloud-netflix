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

package org.springframework.cloud.netflix.eureka.config;

import java.util.Arrays;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.cloud.commons.util.UtilAutoConfiguration;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.DiscoveryClientConfigServiceBootstrapConfiguration;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClientConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.springframework.cloud.config.client.ConfigClientProperties.Discovery.DEFAULT_CONFIG_SERVER;

/**
 * @author Dave Syer
 */
public class DiscoveryClientConfigServiceAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

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
		setup("spring.cloud.config.discovery.enabled=true",
				"eureka.instance.metadataMap.foo:bar",
				"eureka.instance.nonSecurePort:7001", "eureka.instance.hostname:foo");
		assertEquals(1, this.context.getBeanNamesForType(
				EurekaDiscoveryClientConfigServiceAutoConfiguration.class).length);
		EurekaClient eurekaClient = this.context.getParent().getBean(EurekaClient.class);
		Mockito.verify(eurekaClient, times(2)).getInstancesByVipAddress(DEFAULT_CONFIG_SERVER,
				false);
		Mockito.verify(eurekaClient, times(1)).shutdown();
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
		parent.register(UtilAutoConfiguration.class,
				EurekaDiscoveryClientConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, EnvironmentKnobbler.class,
				EurekaDiscoveryClientConfigServiceBootstrapConfiguration.class,
				DiscoveryClientConfigServiceBootstrapConfiguration.class,
				ConfigClientProperties.class);
		parent.refresh();
		this.context = new AnnotationConfigApplicationContext();
		this.context.setParent(parent);
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				EurekaDiscoveryClientConfigServiceAutoConfiguration.class,
				EurekaClientAutoConfiguration.class);
		this.context.refresh();
	}

	@Configuration
	protected static class EnvironmentKnobbler {

		@Bean
		public EurekaClient eurekaClient(ApplicationInfoManager manager) {
			InstanceInfo info = manager.getInfo();
			EurekaClient client = Mockito.mock(CloudEurekaClient.class);
			given(client.getInstancesByVipAddress(DEFAULT_CONFIG_SERVER, false))
					.willReturn(Arrays.asList(info));
			return client;
		}

	}

}
