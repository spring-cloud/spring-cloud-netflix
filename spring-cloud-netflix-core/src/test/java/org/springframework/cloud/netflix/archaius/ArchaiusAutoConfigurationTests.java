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

package org.springframework.cloud.netflix.archaius;

import java.util.Collections;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.junit.After;
import org.junit.Test;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.netflix.config.ConfigurationManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Dave Syer
 */
public class ArchaiusAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;
	private Object propertyValue;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void configurationCreated() {
		this.context = new AnnotationConfigApplicationContext(
				ArchaiusAutoConfiguration.class);
		AbstractConfiguration config = this.context
				.getBean(ConfigurableEnvironmentConfiguration.class);
		assertNotNull(config.getString("java.io.tmpdir"));
	}

	@Test
	public void environmentChangeEventPropagated() {
		this.context = new AnnotationConfigApplicationContext(
				ArchaiusAutoConfiguration.class);
		ConfigurationManager.getConfigInstance().addConfigurationListener(
				new ConfigurationListener() {
					@Override
					public void configurationChanged(ConfigurationEvent event) {
						if (event.getPropertyName().equals("my.prop")) {
							ArchaiusAutoConfigurationTests.this.propertyValue = event
									.getPropertyValue();
						}
					}
				});
		EnvironmentTestUtils.addEnvironment(this.context, "my.prop=my.newval");
		this.context.publishEvent(new EnvironmentChangeEvent(Collections
				.singleton("my.prop")));
		assertEquals("my.newval", this.propertyValue);
	}

}
