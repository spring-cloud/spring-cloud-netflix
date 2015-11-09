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

package org.springframework.cloud.netflix.metrics;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.test.ImportAutoConfiguration;
import org.springframework.cloud.netflix.metrics.servo.ServoMetricsAutoConfiguration;
import org.springframework.cloud.netflix.metrics.servo.ServoMonitorCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

import com.netflix.servo.MonitorRegistry;
import com.netflix.servo.monitor.BasicTimer;
import com.netflix.servo.monitor.MonitorConfig;

/**
 * @author Jon Schneider
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { MetricsRestTemplateRestTemplateConfig.class,
		MetricsRestTemplateTestConfig.class })
@TestPropertySource(properties = { "netflix.metrics.restClient.metricName=metricName",
		"spring.aop.proxy-target-class=true" })
public class MetricsClientHttpRequestInterceptorTests extends AbstractMetricsTests {
	@Autowired
	MonitorRegistry registry;

	@Autowired
	RestTemplate restTemplate;

	@Test
	public void metricsGatheredWhenSuccessful() {
		MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
		mockServer.expect(MockRestRequestMatchers.requestTo("/test/123"))
				.andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
				.andRespond(MockRestResponseCreators.withSuccess("{\"status\" : \"OK\"}", MediaType.APPLICATION_JSON));
		restTemplate.getForObject("/test/{id}", String.class, 123);

		MonitorConfig.Builder builder = new MonitorConfig.Builder("metricName")
				.withTag("method", "GET")
				.withTag("uri", "_test_-id-")
				.withTag("status", "200")
				.withTag("clientName", "none");

		BasicTimer timer = ServoMonitorCache.getTimer(builder.build());

		Assert.assertEquals(1L, (long) timer.getCount());
		mockServer.verify();
	}
}

@Configuration
@ImportAutoConfiguration({ ServoMetricsAutoConfiguration.class,
		PropertyPlaceholderAutoConfiguration.class, AopAutoConfiguration.class })
class MetricsRestTemplateTestConfig {
}

@Configuration
class MetricsRestTemplateRestTemplateConfig {
	@Bean
	RestTemplate restTemplate() {
		return new RestTemplate();
	}
}