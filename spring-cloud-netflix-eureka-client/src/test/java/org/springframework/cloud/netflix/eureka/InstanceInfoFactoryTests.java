/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.eureka;

import java.io.IOException;

import com.netflix.appinfo.InstanceInfo;
import org.junit.Test;

import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

public class InstanceInfoFactoryTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	public void instanceIdIsHostNameByDefault() throws IOException {
		InstanceInfo instanceInfo = setupInstance();
		try (InetUtils utils = new InetUtils(new InetUtilsProperties())) {
			assertThat(instanceInfo.getId())
					.isEqualTo(utils.findFirstNonLoopbackHostInfo().getHostname());
		}
	}

	@Test
	public void instanceIdIsIpWhenIpPreferred() throws Exception {
		InstanceInfo instanceInfo = setupInstance("eureka.instance.preferIpAddress:true");
		assertThat(instanceInfo.getId().matches("(\\d+\\.){3}\\d+")).isTrue();
	}

	@Test
	public void instanceInfoIdIsInstanceIdWhenSet() {
		InstanceInfo instanceInfo = setupInstance("eureka.instance.instanceId:special");
		assertThat(instanceInfo.getId()).isEqualTo("special");
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
