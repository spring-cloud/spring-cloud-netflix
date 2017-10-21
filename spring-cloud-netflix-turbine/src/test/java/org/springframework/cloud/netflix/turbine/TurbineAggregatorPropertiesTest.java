package org.springframework.cloud.netflix.turbine;

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.util.EnvironmentTestUtils.addEnvironment;

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
		addEnvironment(this.context,
				"turbine.aggregator.clusterConfig=cluster1, cluster2, cluster3");
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