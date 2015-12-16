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

package org.springframework.cloud.netflix.metrics.atlas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

import com.netflix.servo.Metric;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.tag.BasicTagList;

import static com.netflix.servo.annotations.DataSourceType.COUNTER;
import static com.netflix.servo.annotations.DataSourceType.GAUGE;
import static com.netflix.servo.annotations.DataSourceType.INFORMATIONAL;
import static com.netflix.servo.annotations.DataSourceType.KEY;
import static com.netflix.servo.annotations.DataSourceType.NORMALIZED;
import static com.netflix.servo.annotations.DataSourceType.RATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Jon Schneider
 */
public class AtlasMetricObserverTests {
	@Test
	public void normalizeAtlasUri() {
		String normalized = "http://localhost:7001/api/v1/publish";
		assertEquals(normalized, AtlasMetricObserver.normalizeAtlasUri("http://localhost:7001"));
		assertEquals(normalized, AtlasMetricObserver.normalizeAtlasUri("http://localhost:7001/"));
		assertEquals(normalized, AtlasMetricObserver.normalizeAtlasUri("http://localhost:7001/api/v1/publish"));
		assertEquals(normalized, AtlasMetricObserver.normalizeAtlasUri("http://localhost:7001/api/v1/publish/"));
	}

	@Test(expected = IllegalStateException.class)
	public void emptyAtlasUriThrowsException() {
		AtlasMetricObserver.normalizeAtlasUri("");
	}

	@Test(expected = IllegalStateException.class)
	public void missingAtlasUriThrowsException() {
		AtlasMetricObserver.normalizeAtlasUri(null);
	}

	@Test
	public void checkValidityOfTags() {
		assertTrue(AtlasMetricObserver.validTags(BasicTagList.of("foo", "bar")));
		assertFalse(AtlasMetricObserver.validTags(BasicTagList.of("{foo}", "bar")));
		assertFalse(AtlasMetricObserver.validTags(BasicTagList.of("foo", "{bar}")));
	}

	@Test
	public void assignTypesToMetrics() {
		assertHasAtlasType("counter", metricWithType("foo", COUNTER));

		assertHasAtlasType("gauge", metricWithType("foo", GAUGE));
		assertHasAtlasType("gauge", metricWithType("foo", NORMALIZED));
		assertHasAtlasType("gauge", metricWithType("foo", RATE));

		assertHasAtlasType("rate", metricWithType("foo", INFORMATIONAL));
		assertHasAtlasType("rate", new Metric(new MonitorConfig.Builder("foo").build(),
				0, "bar"));

		// already has type
		Metric m = new Metric(new MonitorConfig.Builder("foo")
				.withTag(KEY, COUNTER.name()).withTag("atlas.dstype", "counter").build(),
				0, "bar");
		assertHasAtlasType("counter", m);
		assertEquals(2, m.getConfig().getTags().size());
	}

	private void assertHasAtlasType(String atlasType, Metric m) {
		assertEquals(atlasType,
				AtlasMetricObserver.addTypeTagsAsNecessary(Collections.singletonList(m))
						.get(0).getConfig().getTags().getValue("atlas.dstype"));
	}

	private Metric metricWithType(String key, DataSourceType type) {
		return new Metric(new MonitorConfig.Builder(key).withTag(KEY, type.name())
				.build(), 0, 1);
	}

	@Test
	public void metricsSentInBatches() {
		RestTemplate restTemplate = new RestTemplate();

		AtlasMetricObserverConfigBean config = new AtlasMetricObserverConfigBean();
		config.setBatchSize(2);
		config.setUri("atlas");

		AtlasMetricObserver obs = new AtlasMetricObserver(config, restTemplate,
				BasicTagList.EMPTY);

		// batch size is divisible by metric size
		MockRestServiceServer mockServer = MockRestServiceServer
				.createServer(restTemplate);
		expectTotalBatches(mockServer, 2);
		obs.update(generateMetrics(4));
		mockServer.verify();

		// batch size is not divisible by metric size
		mockServer = MockRestServiceServer.createServer(restTemplate);
		expectTotalBatches(mockServer, 3);
		obs.update(generateMetrics(5));
		mockServer.verify();

		// metric size is less than batch size
		mockServer = MockRestServiceServer.createServer(restTemplate);
		expectTotalBatches(mockServer, 1);
		obs.update(generateMetrics(1));
		mockServer.verify();

		// no metrics to send
		mockServer = MockRestServiceServer.createServer(restTemplate);
		expectTotalBatches(mockServer, 0);
		obs.update(Collections.<Metric> emptyList());
		mockServer.verify();

		// a single non-numeric metric does not result in a post
		mockServer = MockRestServiceServer.createServer(restTemplate);
		expectTotalBatches(mockServer, 0);
		obs.update(Collections.singletonList(new Metric(new MonitorConfig.Builder("foo")
				.build(), 0, "nonumber")));
		mockServer.verify();
	}

	private List<Metric> generateMetrics(int numberOfMetrics) {
		List<Metric> metrics = new ArrayList<>();
		for (int i = 0; i < numberOfMetrics; i++)
			metrics.add(metricWithType("foo" + i, DataSourceType.GAUGE));
		return metrics;
	}

	private void expectTotalBatches(MockRestServiceServer mockServer,
			int totalBatchesExpected) {
		for (int i = 0; i < totalBatchesExpected; i++) {
			mockServer
					.expect(MockRestRequestMatchers.requestTo("atlas/api/v1/publish"))
					.andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
					.andRespond(
							MockRestResponseCreators.withSuccess("{\"status\" : \"OK\"}",
									MediaType.APPLICATION_JSON));
		}
	}
}
