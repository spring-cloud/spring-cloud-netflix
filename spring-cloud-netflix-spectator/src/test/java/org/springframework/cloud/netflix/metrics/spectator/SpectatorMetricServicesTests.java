/*
 * Copyright 2013-2015 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.cloud.netflix.metrics.spectator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.springframework.cloud.netflix.metrics.spectator.SpectatorMetricServices.stripMetricName;

public class SpectatorMetricServicesTests {
	@Test
	public void metricNameWithNoTypeIsLeftUnchanged() {
		assertEquals("foo", stripMetricName("foo"));
		assertEquals("foo.bar", stripMetricName("foo.bar"));
	}

	@Test
	public void metricTypeIsStrippedFromMetricName() {
		assertEquals("foo", stripMetricName("timer.foo"));
		assertEquals("foo", stripMetricName("histogram.foo"));
		assertEquals("foo", stripMetricName("meter.foo"));
	}

	@Test
	public void metricTypeNameEmbeddedInMiddleOfMetricNameIsNotRemoved() {
		assertEquals("bar.timer.foo", stripMetricName("bar.timer.foo"));
	}
}
