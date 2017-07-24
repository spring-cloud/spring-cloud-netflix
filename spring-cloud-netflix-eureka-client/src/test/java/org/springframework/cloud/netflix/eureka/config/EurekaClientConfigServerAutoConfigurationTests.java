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

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.config.server.config.ConfigServerProperties;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.cloud.netflix.eureka.config.EurekaClientConfigServerAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.netflix.appinfo.EurekaInstanceConfig;

import static org.junit.Assert.assertEquals;

/**
 * @author Dave Syer
 */
public class EurekaClientConfigServerAutoConfigurationTests {

	private ConfigurableApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void offByDefault() throws Exception {
		this.context = new AnnotationConfigApplicationContext(
				EurekaClientConfigServerAutoConfiguration.class);
		assertEquals(0,
				this.context.getBeanNamesForType(EurekaInstanceConfigBean.class).length);
	}

	@Test
	public void onWhenRequested() throws Exception {
		setup("spring.cloud.config.server.prefix=/config");
		assertEquals(1,
				this.context.getBeanNamesForType(EurekaInstanceConfig.class).length);
		EurekaInstanceConfig instance = this.context.getBean(EurekaInstanceConfig.class);
		assertEquals("/config", instance.getMetadataMap().get("configPath"));
	}

	private void setup(String... env) {
		this.context = new SpringApplicationBuilder(
				PropertyPlaceholderAutoConfiguration.class,
				EurekaClientConfigServerAutoConfiguration.class,
				ConfigServerProperties.class, EurekaInstanceConfigBean.class).web(false)
				.properties(env).run();
	}

}
