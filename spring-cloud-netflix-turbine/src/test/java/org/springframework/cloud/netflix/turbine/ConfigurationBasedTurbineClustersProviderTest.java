/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.netflix.turbine;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigurationBasedTurbineClustersProviderTest {

	@Test
	public void shouldReturnDefaultClusterIfConfigurationIsEmpty() throws Exception {
		TurbineAggregatorProperties properties = new TurbineAggregatorProperties();
		TurbineClustersProvider provider = new ConfigurationBasedTurbineClustersProvider(
				properties);
		List<String> clusterNames = provider.getClusterNames();

		assertThat(clusterNames).containsOnly("default");
	}

	@Test
	public void shouldReturnConfiguredClusters() throws Exception {
		TurbineAggregatorProperties properties = new TurbineAggregatorProperties();
		properties.setClusterConfig(Arrays.asList("cluster1", "cluster2", "cluster3"));
		TurbineClustersProvider provider = new ConfigurationBasedTurbineClustersProvider(
				properties);
		List<String> clusterNames = provider.getClusterNames();

		assertThat(clusterNames).containsOnly("cluster1", "cluster2", "cluster3");
	}

}
