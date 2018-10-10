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

package org.springframework.cloud.netflix.turbine;

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

public class TurbineAggregatorPropertiesTest {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@After
	public void clear() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void shouldHaveDefaultConfiguration() throws Exception {
		setupContext();

		TurbineAggregatorProperties actual = getProperties();
		assertThat(actual.getClusterConfig()).containsOnly("default");
	}

	@Test
	public void shouldLoadCustomProperties() {
		TestPropertyValues.of(
				"turbine.aggregator.clusterConfig=cluster1, cluster2, cluster3").applyTo(this.context);
		setupContext();

		TurbineAggregatorProperties actual = getProperties();
		assertThat(actual.getClusterConfig()).containsOnly("cluster1", "cluster2",
				"cluster3");
	}

	private void setupContext() {
		this.context.register(TestConfiguration.class);
		this.context.refresh();
	}

	private TurbineAggregatorProperties getProperties() {
		return this.context.getBean(TurbineAggregatorProperties.class);
	}

	@Configuration
	@EnableConfigurationProperties(TurbineAggregatorProperties.class)
	static class TestConfiguration {
	}
}