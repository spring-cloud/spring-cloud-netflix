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

package org.springframework.cloud.netflix.eureka;

import java.io.IOException;

import org.junit.Test;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.appinfo.InstanceInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InstanceInfoFactoryTests {
	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	public void instanceIdIsHostNameByDefault() throws IOException {
		InstanceInfo instanceInfo = setupInstance();
		try (InetUtils utils = new InetUtils(new InetUtilsProperties())) {
			assertEquals(utils.findFirstNonLoopbackHostInfo().getHostname(),
					instanceInfo.getId());
		}
	}

	@Test
	public void instanceIdIsIpWhenIpPreferred() throws Exception {
		InstanceInfo instanceInfo = setupInstance("eureka.instance.preferIpAddress:true");
		assertTrue(instanceInfo.getId().matches("(\\d+\\.){3}\\d+"));
	}

	@Test
	public void instanceInfoIdIsInstanceIdWhenSet() {
		InstanceInfo instanceInfo = setupInstance("eureka.instance.instanceId:special");
		assertEquals("special", instanceInfo.getId());
	}

	private InstanceInfo setupInstance(String... pairs) {
		TestPropertyValues.of(pairs).applyTo(this.context);

		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				TestConfiguration.class);
		this.context.refresh();

		EurekaInstanceConfigBean instanceConfig = getInstanceConfig();
		return new InstanceInfoFactory().create(instanceConfig);
	}

	private EurekaInstanceConfigBean getInstanceConfig() {
		return this.context.getBean(EurekaInstanceConfigBean.class);
	}

	@Configuration
	@EnableConfigurationProperties
	protected static class TestConfiguration {
		@Bean
		public EurekaInstanceConfigBean eurekaInstanceConfigBean() {
			return new EurekaInstanceConfigBean(new InetUtils(new InetUtilsProperties()));
		}
	}
}
