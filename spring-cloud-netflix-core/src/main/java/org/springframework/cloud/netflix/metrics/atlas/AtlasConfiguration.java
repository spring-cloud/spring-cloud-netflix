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
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.metrics.export.Exporter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.netflix.metrics.servo.ServoMetricsAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;

import com.netflix.servo.MonitorRegistry;
import com.netflix.servo.publish.MonitorRegistryMetricPoller;
import com.netflix.servo.tag.BasicTagList;

/**
 * Configures the Atlas metrics backend, also configuring Spectator to collect metrics if necessary.
 *
 * @author Jon Schneider
 */
@Configuration
@ConditionalOnClass(AtlasMetricObserver.class)
@Import(ServoMetricsAutoConfiguration.class)
public class AtlasConfiguration {
	@Autowired(required = false)
	private Collection<AtlasTagProvider> tagProviders;

	@Autowired(required = false)
	@Qualifier("atlasRestTemplate")
	private RestTemplate restTemplate = new RestTemplate();

	@Bean
	public AtlasMetricObserverConfigBean atlasObserverConfig() {
		return new AtlasMetricObserverConfigBean();
	}

	@Bean
	@ConditionalOnMissingBean
	public AtlasMetricObserver atlasObserver(AtlasMetricObserverConfigBean atlasObserverConfig) {
		BasicTagList tags = (BasicTagList) BasicTagList.EMPTY;
		if (tagProviders != null) {
			for (AtlasTagProvider tagProvider : tagProviders) {
				for (Map.Entry<String, String> tag : tagProvider.defaultTags().entrySet()) {
					if (tag.getValue() != null)
						tags = tags.copy(tag.getKey(), tag.getValue());
				}
			}
		}
		return new AtlasMetricObserver(atlasObserverConfig, restTemplate, tags);
	}

	@Bean
	@ConditionalOnMissingBean
	public Exporter exporter(AtlasMetricObserver observer, MonitorRegistry monitorRegistry) {
		return new AtlasExporter(observer, new MonitorRegistryMetricPoller(monitorRegistry));
	}
}
