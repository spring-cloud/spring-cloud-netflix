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

package org.springframework.cloud.netflix.metrics.servo;

import org.junit.Test;

import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.monitor.AbstractMonitor;
import com.netflix.servo.monitor.MonitorConfig;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Spencer Gibb
 */
public class HierarchicalServoMetricNamingTests {

	private HierarchicalServoMetricNaming naming = new HierarchicalServoMetricNaming();

	@Test
	public void vanillaServoMetricWorks() {
		MonitorConfig config = MonitorConfig.builder("testMetric").build();
		String name = naming.asHierarchicalName(new FixedValueMonitor<>(config, 0));
		assertThat(name, is(equalTo("servo.testmetric")));
	}

	@Test
	public void nameWithPeriodWorks() {
		MonitorConfig config = MonitorConfig.builder("test.Metric").build();
		String name = naming.asHierarchicalName(new FixedValueMonitor<>(config, 0));
		assertThat(name, is(equalTo("servo.test.metric")));
	}

	@Test
	public void typeTagWorks() {
		MonitorConfig config = MonitorConfig.builder("testMetric")
				.withTag(DataSourceType.KEY, DataSourceType.COUNTER.getValue()).build();

		String name = naming.asHierarchicalName(new FixedValueMonitor<>(config, 0));
		assertThat(name, is(equalTo("counter.servo.testmetric")));
	}

	@Test
	public void instanceTagWorks() {
		MonitorConfig config = MonitorConfig.builder("testMetric")
				.withTag("instance", "instance0").build();
		String name = naming.asHierarchicalName(new FixedValueMonitor<>(config, 0));
		assertThat(name, is(equalTo("servo.instance0.testmetric")));
	}

	@Test
	public void statisticTagWorks() {
		MonitorConfig config = MonitorConfig.builder("testMetric")
				.withTag("statistic", "min").build();
		String name = naming.asHierarchicalName(new FixedValueMonitor<>(config, 0));
		assertThat(name, is(equalTo("servo.testmetric.min")));
	}

	@Test
	public void allTagsWork() {
		MonitorConfig config = MonitorConfig.builder("testMetric")
				.withTag(DataSourceType.KEY, DataSourceType.COUNTER.getValue())
				.withTag("instance", "instance0").withTag("statistic", "min").build();
		String name = naming.asHierarchicalName(new FixedValueMonitor<>(config, 0));
		assertThat(name, is(equalTo("counter.servo.instance0.testmetric.min")));
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