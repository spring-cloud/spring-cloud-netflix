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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.metrics.export.Exporter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.netflix.metrics.spectator.SpectatorAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.frigga.Names;
import com.netflix.servo.publish.MetricPoller;
import com.netflix.servo.publish.atlas.AtlasMetricObserver;
import com.netflix.servo.publish.atlas.ServoAtlasConfig;
import com.netflix.servo.tag.BasicTagList;

/**
 * Configures the Atlas metrics backend, also configuring Spectator to collect metrics if necessary.
 *
 * @author Jon Schneider
 */
@Configuration
@ConditionalOnClass(AtlasMetricObserver.class)
@Import(SpectatorAutoConfiguration.class)
public class AtlasAutoConfiguration implements ServoAtlasConfig {
	@Autowired(required = false)
	private Collection<AtlasTagProvider> tagProviders;

	@Value("${netflix.atlas.uri}")
	private String uri;

	@Value("${netflix.atlas.pushQueueSize:1000}")
	private Integer pushQueueSize;

	@Value("${netflix.atlas.enabled:true}")
	private boolean enabled;

	@Value("${netflix.atlas.batchSize:10000}")
	private Integer batchSize;

	@Bean
	@ConditionalOnMissingBean(AtlasMetricObserver.class)
	public AtlasMetricObserver atlasObserver() {
		BasicTagList tags = (BasicTagList) BasicTagList.EMPTY;
		if (tagProviders != null) {
			for (AtlasTagProvider tagProvider : tagProviders) {
				for (Map.Entry<String, String> tag : tagProvider.defaultTags().entrySet()) {
					if (tag.getValue() != null)
						tags = tags.copy(tag.getKey(), tag.getValue());
				}
			}
		}
		return new AtlasMetricObserver(this, tags);
	}

	@Bean
	@ConditionalOnMissingBean(Exporter.class)
	public AtlasExporter exporter(AtlasMetricObserver observer, MetricPoller poller) {
		return new AtlasExporter(observer, poller);
	}

	@Override
	public String getAtlasUri() {
		return uri;
	}

	@Override
	public int getPushQueueSize() {
		return pushQueueSize;
	}

	@Override
	public boolean shouldSendMetrics() {
		return enabled;
	}

	@Override
	public int batchSize() {
		return batchSize;
	}

	@Configuration
	@ConditionalOnBean(InstanceInfo.class)
	static class InstanceInfoTagProviderConfiguration {
		@Autowired
		InstanceInfo instanceInfo;

		@Bean
		public AtlasTagProvider instanceInfoTags() {
			return () -> {
				Map<String, String> tags = new HashMap<>();
				tags.put("nf.app", instanceInfo.getAppName());
				tags.put("nf.cluster", Names.parseName(instanceInfo.getASGName()).getCluster());
				return tags;
			};
		}
	}
}
