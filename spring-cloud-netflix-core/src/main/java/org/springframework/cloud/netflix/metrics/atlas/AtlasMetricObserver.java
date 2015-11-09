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

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.netflix.servo.Metric;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.publish.MetricObserver;
import com.netflix.servo.tag.BasicTag;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.TagList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Observer that forwards metrics to atlas. In addition to being a MetricObserver, it also supports a push model that
 * sends metrics as soon as possible (asynchronously).
 *
 * @author Jon Schneider
 */
public class AtlasMetricObserver implements MetricObserver {
	private static final Log logger = LogFactory.getLog(AtlasMetricObserver.class);
	private static final SmileFactory smileFactory = new SmileFactory();
	private static final Tag atlasRateTag = new BasicTag("atlas.dstype", "rate");
	private static final Tag atlasCounterTag = new BasicTag("atlas.dstype", "counter");
	private static final Tag atlasGaugeTag = new BasicTag("atlas.dstype", "gauge");
	private static final Pattern validAtlasTag = Pattern.compile("[\\.\\-\\w]+");

	private AtlasMetricObserverConfigBean config;
	private RestTemplate restTemplate;
	private TagList commonTags;
	private String uri;

	public AtlasMetricObserver(AtlasMetricObserverConfigBean config, RestTemplate restTemplate, TagList commonTags) {
		this.config = config;
		this.commonTags = commonTags;
		this.restTemplate = restTemplate;
		this.uri = normalizeAtlasUri(config.getUri());

		if (!validTags(commonTags)) {
			throw new IllegalArgumentException(
					"One or more atlas tags contain invalid characters, must match [\\.\\-\\w]+");
		}
	}

	@Override
	public String getName() {
		return "atlas";
	}

	protected static boolean validTags(TagList tags) {
		for (Tag tag : tags) {
			if (!validAtlasTag.matcher(tag.getKey()).matches()) {
				logger.error("Invalid tag key " + tag.getKey());
				return false;
			}

			if (!validAtlasTag.matcher(tag.getValue()).matches()) {
				logger.error("Invalid tag value " + tag.getValue());
				return false;
			}
		}

		return true;
	}

	protected static String normalizeAtlasUri(String uri) {
		Matcher matcher = Pattern.compile("(.+?)(/api/v1/publish)?/?").matcher(uri);
		matcher.matches();
		return matcher.group(1) + "/api/v1/publish";
	}

	@Override
	public void update(List<Metric> rawMetrics) {
		if (!config.isEnabled()) {
			logger.debug("Atlas metric observer disabled. Not sending metrics.");
			return;
		}

		if (rawMetrics.isEmpty()) {
			logger.debug("Metrics list is empty, no data being sent to server.");
			return;
		}

		List<Metric> metrics = addTypeTagsAsNecessary(rawMetrics);

		for (int i = 0; i < metrics.size(); i += config.getBatchSize()) {
			List<Metric> batch = metrics.subList(i, Math.min(metrics.size(), config.getBatchSize() + i));
			logger.debug("Sending a metrics batch of size " + batch.size());
			sendMetricsBatch(batch);
		}
	}

	private void sendMetricsBatch(List<Metric> metrics) {
		try {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			JsonGenerator gen = smileFactory.createGenerator(output, JsonEncoding.UTF8);

			gen.writeStartObject();

			writeCommonTags(gen);
			if (writeMetrics(gen, metrics) == 0)
				return; // short circuit this batch if no valid/numeric metrics existed

			gen.writeEndObject();
			gen.flush();

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.valueOf("application/x-jackson-smile"));
			HttpEntity<byte[]> entity = new HttpEntity<>(output.toByteArray(), headers);
			try {
				restTemplate.exchange(uri, HttpMethod.POST, entity, Map.class);
			} catch (HttpClientErrorException e) {
				logger.error("Failed to write metrics to atlas: " + e.getResponseBodyAsString(), e);
			} catch (RestClientException e) {
				logger.error("Failed to write metrics to atlas", e);
			}
		} catch (IOException e) {
			// an IOException stemming from the generator writing to a ByteArrayOutputStream is impossible
			throw new RuntimeException(e);
		}
	}

	private void writeCommonTags(JsonGenerator gen) throws IOException {
		gen.writeObjectFieldStart("tags");
		for (Tag tag : commonTags)
			gen.writeStringField(tag.getKey(), tag.getValue());
		gen.writeEndObject();
	}

	private int writeMetrics(JsonGenerator gen, List<Metric> metrics) throws IOException {
		int totalMetricsInBatch = 0;
		gen.writeArrayFieldStart("metrics");

		for (Metric m : metrics) {
			if (!validTags(m.getConfig().getTags()))
				continue;

			if (!Number.class.isAssignableFrom(m.getValue().getClass()))
				continue;

			gen.writeStartObject();

			gen.writeObjectFieldStart("tags");
			gen.writeStringField("name", m.getConfig().getName());
			for (Tag tag : m.getConfig().getTags())
				gen.writeStringField(tag.getKey(), tag.getValue());
			gen.writeEndObject();

			gen.writeNumberField("start", m.getTimestamp());
			gen.writeNumberField("value", m.getNumberValue().doubleValue());

			gen.writeEndObject();

			totalMetricsInBatch++;
		}

		gen.writeEndArray();
		return totalMetricsInBatch;
	}

	protected static List<Metric> addTypeTagsAsNecessary(List<Metric> metrics) {
		List<Metric> typedMetrics = new ArrayList<>();
		for (Metric m : metrics) {
			String value = m.getConfig().getTags().getValue(DataSourceType.KEY);
			Metric transformed;

			// Atlas will not normalize metrics tagged with atlas.dstype=gauge. Since these metric types are
			// pre-normalized, we do not want Atlas to touch the value
			if (DataSourceType.GAUGE.name().equals(value) || DataSourceType.RATE.name().equals(value)
					|| DataSourceType.NORMALIZED.name().equals(value)) {
				transformed = new Metric(m.getConfig().withAdditionalTag(atlasGaugeTag), m.getTimestamp(), m.getValue());
			}

			// atlas.dstype=counter means you're sending the absolute value of the counter (a monotonically
			// increasing value), and Atlas will keep the previous value and convert it to a rate per second
			// when the metric is received
			else if (DataSourceType.COUNTER.name().equals(value)) {
				transformed = new Metric(m.getConfig().withAdditionalTag(atlasCounterTag), m.getTimestamp(),
						m.getValue());
			}

			// Atlas will normalize the value to a minute boundary based on its timestamp
			else {
				transformed = new Metric(m.getConfig().withAdditionalTag(atlasRateTag), m.getTimestamp(), m.getValue());
			}

			typedMetrics.add(transformed);
		}
		return typedMetrics;
	}
}