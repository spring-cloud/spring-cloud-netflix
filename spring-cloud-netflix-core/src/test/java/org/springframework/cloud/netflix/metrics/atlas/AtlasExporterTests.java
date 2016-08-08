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

import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

import com.netflix.servo.monitor.DynamicCounter;

/**
 * @author Jon Schneider
 */
@SpringBootTest(classes = AtlasExporterConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class AtlasExporterTests {
	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private AtlasExporter atlasExporter;

	@Test
	public void exportMetricsAtPeriodicIntervals() {
		MockRestServiceServer mockServer = MockRestServiceServer
				.createServer(this.restTemplate);

		mockServer.expect(MockRestRequestMatchers.requestTo("atlas/api/v1/publish"))
				.andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
				.andRespond(MockRestResponseCreators.withSuccess("{\"status\" : \"OK\"}",
						MediaType.APPLICATION_JSON));

		DynamicCounter.increment("counterThatWillBeSentToAtlas");
		this.atlasExporter.export();

		mockServer.verify();
	}
}

@EnableAutoConfiguration
@Configuration
@EnableAtlas
class AtlasExporterConfiguration {

	@Qualifier("atlasRestTemplate")
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	public static PropertySourcesPlaceholderConfigurer properties() throws Exception {
		final PropertySourcesPlaceholderConfigurer config = new PropertySourcesPlaceholderConfigurer();
		Properties properties = new Properties();
		properties.setProperty("netflix.atlas.uri", "atlas");
		config.setProperties(properties);
		return config;
	}
}