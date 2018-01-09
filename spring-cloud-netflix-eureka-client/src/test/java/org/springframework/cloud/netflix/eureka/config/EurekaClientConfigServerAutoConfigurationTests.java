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
 */

package org.springframework.cloud.netflix.eureka.config;

import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.config.server.config.ConfigServerProperties;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;

import com.netflix.appinfo.EurekaInstanceConfig;

import static org.junit.Assert.assertEquals;

/**
 * @author Dave Syer
 * @author Biju Kunjummen
 */
public class EurekaClientConfigServerAutoConfigurationTests {

	@Test
	public void offByDefault() {
		new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(EurekaClientConfigServerAutoConfiguration.class))
			.run(c -> {
				assertEquals(0,
					c.getBeanNamesForType(EurekaInstanceConfigBean.class).length);
			});
	}

	@Test
	public void onWhenRequested() {
		new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
				EurekaClientConfigServerAutoConfiguration.class,
				ConfigServerProperties.class, EurekaInstanceConfigBean.class))
			.withPropertyValues("spring.cloud.config.server.prefix=/config")
			.run(c -> {
				assertEquals(1,
					c.getBeanNamesForType(EurekaInstanceConfig.class).length);
				EurekaInstanceConfig instance = c.getBean(EurekaInstanceConfig.class);
				assertEquals("/config", instance.getMetadataMap().get("configPath"));
			});
	}

}