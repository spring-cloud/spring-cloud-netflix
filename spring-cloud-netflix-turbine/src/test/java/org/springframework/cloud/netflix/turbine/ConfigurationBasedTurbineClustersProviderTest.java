package org.springframework.cloud.netflix.turbine;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

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