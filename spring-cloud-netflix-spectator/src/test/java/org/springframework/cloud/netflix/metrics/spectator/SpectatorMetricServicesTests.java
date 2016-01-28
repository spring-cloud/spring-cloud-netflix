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

import com.netflix.spectator.api.DefaultRegistry;
import com.netflix.spectator.api.Measurement;
import com.netflix.spectator.api.Registry;
import org.junit.Test;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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

	@Test
	public void meterPrefixShouldIncrementCounter() {
		Registry registry = new DefaultRegistry();
		SpectatorMetricServices ms = new SpectatorMetricServices(registry);
		ms.increment("meter.test");
		assertEquals(1, registry.counter("test").count());
	}

	@Test
	public void otherPrefixShouldUpdateGauge() {
		Registry registry = new DefaultRegistry();
		SpectatorMetricServices ms = new SpectatorMetricServices(registry);

		ms.increment("gauge.test");
		assertGaugeEquals(registry, "gauge.test", 1.0);

		ms.decrement("gauge.test");
		assertGaugeEquals(registry, "gauge.test", 0.0);
	}

	@Test
	public void histogramSubmit() {
		Registry registry = new DefaultRegistry();
		SpectatorMetricServices ms = new SpectatorMetricServices(registry);
		ms.submit("histogram.test", 42.0);
		assertEquals(1L, registry.distributionSummary("test").count());
		assertEquals(42L, registry.distributionSummary("test").totalAmount());
	}

	@Test
	public void timerSubmit() {
		Registry registry = new DefaultRegistry();
		SpectatorMetricServices ms = new SpectatorMetricServices(registry);
		ms.submit("timer.test", 42.0);
		assertEquals(1L, registry.timer("test").count());
		assertEquals(TimeUnit.MILLISECONDS.toNanos(42L), registry.timer("test").totalTime());
	}

	@Test
	public void timerMicrosSubmit() {
		Registry registry = new DefaultRegistry();
		SpectatorMetricServices ms = new SpectatorMetricServices(registry);
		ms.submit("timer.test", 0.042);
		assertEquals(1L, registry.timer("test").count());
		assertEquals(TimeUnit.MICROSECONDS.toNanos(42L), registry.timer("test").totalTime());
	}

	@Test
	public void gaugeSubmit() {
		Registry registry = new DefaultRegistry();
		SpectatorMetricServices ms = new SpectatorMetricServices(registry);
		ms.submit("gauge.test", 42.0);
		assertGaugeEquals(registry, "gauge.test", 42.0);

		ms.submit("gauge.test", 1.0);
		assertGaugeEquals(registry, "gauge.test", 1.0);
	}

	@Test
	public void gaugeSubmitManyTimes() {
		// Sanity check for memory leak reported in:
		// https://github.com/Netflix/spectator/issues/264
		Registry registry = new DefaultRegistry();
		SpectatorMetricServices ms = new SpectatorMetricServices(registry);
		for (int i = 0; i < 10000; ++i) {
			ms.submit("gauge.test", 42.0);
			assertGaugeEquals(registry, "gauge.test", 42.0);
		}
	}

	private void assertGaugeEquals(Registry registry, String name, double expected) {
		Iterator<Measurement> it = registry.get(registry.createId(name)).measure().iterator();
		assertTrue(it.hasNext());
		assertEquals(expected, it.next().value(), 1e-3);
		assertTrue(!it.hasNext());
	}
}
