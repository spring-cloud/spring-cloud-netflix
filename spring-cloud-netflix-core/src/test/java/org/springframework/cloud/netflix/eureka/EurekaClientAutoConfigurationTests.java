/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.cloud.netflix.eureka;

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.junit.Assert.assertEquals;
import static org.springframework.boot.test.EnvironmentTestUtils.addEnvironment;

/**
 * @author Spencer Gibb
 */
public class EurekaClientAutoConfigurationTests {
	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@After
	public void init() {
		if (this.context != null) {
			this.context.close();
		}
	}

	private void setupContext() {
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				TestConfiguration.class);
		this.context.refresh();
	}

	@Test
	public void nonSecurePort2() {
		testNonSecurePort("server.port");
	}

	@Test
	public void nonSecurePort3() {
		testNonSecurePort("SERVER_PORT");
	}

	@Test
	public void nonSecurePort4() {
		testNonSecurePort("PORT");
	}

	private void testNonSecurePort(String propName) {
		addEnvironment(this.context, propName + ":8888");
		setupContext();
		assertEquals(8888, getInstanceConfig().getNonSecurePort());
	}

	private EurekaInstanceConfigBean getInstanceConfig() {
		return this.context.getBean(EurekaInstanceConfigBean.class);
	}

	@Configuration
	@EnableConfigurationProperties
	@Import(EurekaClientAutoConfiguration.class)
	protected static class TestConfiguration {

	}
}
