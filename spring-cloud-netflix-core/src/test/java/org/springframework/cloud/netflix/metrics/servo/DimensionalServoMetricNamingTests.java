package org.springframework.cloud.netflix.metrics.servo;

import org.junit.Test;

import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.monitor.AbstractMonitor;
import com.netflix.servo.monitor.MonitorConfig;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class DimensionalServoMetricNamingTests {
	private DimensionalServoMetricNaming naming = new DimensionalServoMetricNaming();

	@Test
	public void vanillaServoMetricWorks() {
		MonitorConfig config = MonitorConfig.builder("testMetric").build();
		String name = naming.asHierarchicalName(new FixedValueMonitor<>(config, 0));
		assertThat(name, is(equalTo("testMetric()")));
	}

	@Test
	public void nameWithPeriodWorks() {
		MonitorConfig config = MonitorConfig.builder("test.Metric").build();
		String name = naming.asHierarchicalName(new FixedValueMonitor<>(config, 0));
		assertThat(name, is(equalTo("test.Metric()")));
	}

	@Test
	public void typeTagWorks() {
		MonitorConfig config = MonitorConfig.builder("testMetric")
				.withTag(DataSourceType.KEY, DataSourceType.COUNTER.getValue()).build();

		String name = naming.asHierarchicalName(new FixedValueMonitor<>(config, 0));
		assertThat(name, is(equalTo("testMetric(type=COUNTER)")));
	}

	@Test
	public void instanceTagWorks() {
		MonitorConfig config = MonitorConfig.builder("testMetric")
				.withTag("instance", "instance0").build();
		String name = naming.asHierarchicalName(new FixedValueMonitor<>(config, 0));
		assertThat(name, is(equalTo("testMetric(instance=instance0)")));
	}

	@Test
	public void statisticTagWorks() {
		MonitorConfig config = MonitorConfig.builder("testMetric")
				.withTag("statistic", "min").build();
		String name = naming.asHierarchicalName(new FixedValueMonitor<>(config, 0));
		assertThat(name, is(equalTo("testMetric(statistic=min)")));
	}

	@Test
	public void allTagsWork() {
		MonitorConfig config = MonitorConfig.builder("testMetric")
				.withTag(DataSourceType.KEY, DataSourceType.COUNTER.getValue())
				.withTag("instance", "instance0").withTag("statistic", "min").build();
		String name = naming.asHierarchicalName(new FixedValueMonitor<>(config, 0));
		assertThat(name,
				is(equalTo("testMetric(instance=instance0,statistic=min,type=COUNTER)")));
	}

	private class FixedValueMonitor<T> extends AbstractMonitor<T> {
		T value;

		protected FixedValueMonitor(MonitorConfig config, T value) {
			super(config);
			this.value = value;
		}

		@Override
		public T getValue(int pollerIndex) {
			return value;
		}
	}
}
