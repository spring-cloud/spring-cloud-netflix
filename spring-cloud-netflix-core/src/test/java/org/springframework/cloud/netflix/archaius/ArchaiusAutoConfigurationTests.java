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

package org.springframework.cloud.netflix.archaius;

import java.util.Collections;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.junit.After;
import org.junit.Test;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;

import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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

	@Test
	public void configurationWithoutExternalConfigurations() throws Exception {
		this.context = new AnnotationConfigApplicationContext(
				ArchaiusAutoConfiguration.class);
		DynamicStringProperty dbProperty = DynamicPropertyFactory.getInstance()
				.getStringProperty("db.property", null);
		DynamicStringProperty staticProperty = DynamicPropertyFactory.getInstance()
				.getStringProperty("archaius.file.property", null);

		assertNull(dbProperty.getValue());
		assertNotNull(staticProperty.getValue());
		assertEquals("Static config file property", staticProperty.getValue());
	}

	@Test
	public void configurationWithInjectedConfiguration() throws Exception {
		this.context = new AnnotationConfigApplicationContext(
				ArchaiusAutoConfiguration.class, TestArchaiusExternalConfiguration.class);
		DynamicStringProperty dbProperty = DynamicPropertyFactory.getInstance()
				.getStringProperty("db.property", null);
		DynamicStringProperty secondDbProperty = DynamicPropertyFactory.getInstance()
				.getStringProperty("db.second.property", null);
		DynamicStringProperty staticProperty = DynamicPropertyFactory.getInstance()
				.getStringProperty("archaius.file.property", null);

		assertNotNull(dbProperty.getValue());
		assertNotNull(secondDbProperty.getValue());
		assertNotNull(staticProperty.getValue());
		assertEquals("this is a db property", dbProperty.getValue());
		assertEquals("this is another db property", secondDbProperty.getValue());
		assertEquals("Static config file property", staticProperty.getValue());
	}

}
