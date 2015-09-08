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

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.export.Exporter;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.test.ImportAutoConfiguration;
import org.springframework.cloud.netflix.metrics.spectator.SpectatorAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Timer;

/**
 * @author Jon Schneider
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AtlasTestConfig.class)
@WebAppConfiguration
@TestPropertySource(properties = "netflix.atlas.uri=http://localhost:7102/api/v1/publish")
public class AtlasAutoConfigurationIntegrationTests {
	@Autowired
	Registry registry;

	@Autowired
	Exporter exporter;

	@Test
	public void metricsAreSentToAtlasPeriodically() throws InterruptedException {
		Timer t = registry.timer("t");

		for (int i = 0; i < 1000; i++)
			t.record(100, TimeUnit.MILLISECONDS);
		Thread.sleep(1500);

		Mockito.verify(exporter, Mockito.atLeastOnce()).export();
	}
}

@Configuration
@EnableWebMvc
@EnableScheduling
@ImportAutoConfiguration({ SpectatorAutoConfiguration.class, AtlasAutoConfiguration.class,
		PropertyPlaceholderAutoConfiguration.class, ConfigurationPropertiesAutoConfiguration.class })
class AtlasTestConfig {
	@Autowired
	Exporter exporter;

	@Bean
	Exporter exporter() {
		return Mockito.mock(Exporter.class);
	}

	@Scheduled(fixedRate = 1000L)
	void pushMetricsToAtlas() {
		exporter.export();
	}
}