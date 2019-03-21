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

package org.springframework.cloud.netflix.archaius;

import java.util.Collections;

import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import org.apache.commons.configuration.AbstractConfiguration;
import org.junit.After;
import org.junit.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat(config.getString("java.io.tmpdir")).isNotNull();
	}

	@Test
	public void environmentChangeEventPropagated() {
		this.context = new AnnotationConfigApplicationContext(
				ArchaiusAutoConfiguration.class);
		ConfigurationManager.getConfigInstance().addConfigurationListener(event -> {
			if (event.getPropertyName().equals("my.prop")) {
				ArchaiusAutoConfigurationTests.this.propertyValue = event
						.getPropertyValue();
			}
		});
		TestPropertyValues.of("my.prop=my.newval").applyTo(this.context);
		this.context.publishEvent(
				new EnvironmentChangeEvent(Collections.singleton("my.prop")));
		assertThat(this.propertyValue).isEqualTo("my.newval");
	}

	@Test
	public void configurationWithoutExternalConfigurations() throws Exception {
		this.context = new AnnotationConfigApplicationContext(
				ArchaiusAutoConfiguration.class);
		DynamicStringProperty dbProperty = DynamicPropertyFactory.getInstance()
				.getStringProperty("db.property", null);
		DynamicStringProperty staticProperty = DynamicPropertyFactory.getInstance()
				.getStringProperty("archaius.file.property", null);

		assertThat(dbProperty.getValue()).isNull();
		assertThat(staticProperty.getValue()).isNotNull();
		assertThat(staticProperty.getValue()).isEqualTo("Static config file property");
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

		assertThat(dbProperty.getValue()).isNotNull();
		assertThat(secondDbProperty.getValue()).isNotNull();
		assertThat(staticProperty.getValue()).isNotNull();
		assertThat(dbProperty.getValue()).isEqualTo("this is a db property");
		assertThat(secondDbProperty.getValue()).isEqualTo("this is another db property");
		assertThat(staticProperty.getValue()).isEqualTo("Static config file property");
	}

}
