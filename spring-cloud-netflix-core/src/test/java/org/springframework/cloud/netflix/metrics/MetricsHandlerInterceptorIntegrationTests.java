/*
 * Copyright 2013-2017 the original author or authors.
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

import static org.junit.Assert.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.cloud.netflix.metrics.servo.ServoMetricsAutoConfiguration;
import org.springframework.cloud.netflix.metrics.servo.ServoMonitorCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.netflix.servo.MonitorRegistry;
import com.netflix.servo.monitor.BasicTimer;
import com.netflix.servo.monitor.MonitorConfig;

/**
 * @author Jon Schneider
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = MetricsTestConfig.class)
@WebAppConfiguration
@TestPropertySource(properties = "netflix.metrics.rest.metricName=metricName")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MetricsHandlerInterceptorIntegrationTests {
	@Autowired
	WebApplicationContext webAppContext;

	@Autowired
	MonitorRegistry registry;

	@Autowired
	ServoMonitorCache servoMonitorCache;

	MockMvc mvc;

	@Test
	public void autoConfigurationWiresTheMetricsInterceptor() {
		assertFalse(webAppContext.getBeansOfType(MetricsHandlerInterceptor.class)
				.isEmpty());
	}

	@Before
	public void setup() {
		mvc = MockMvcBuilders.webAppContextSetup(webAppContext).build();
	}

	@Test
	public void metricsGatheredWhenSuccess() throws Exception {
		mvc.perform(get("/test/some/request/10")).andExpect(status().isOk());
		assertTimer("test_some_request_-id-", null, 200);
	}

	@Test
	public void metricsGatheredWhenClientRequestBad() throws Exception {
		mvc.perform(get("/test/some/request/oops"))
				.andExpect(status().is4xxClientError());
		assertTimer("test_some_request_-id-", null, 400);
	}

	@Test
	public void metricsGatheredWhenUnhandledError() throws Exception {
		try {
			mvc.perform(get("/test/some/unhandledError/10")).andExpect(status().isOk());
		}
		catch (Exception e) {
		}
		assertTimer("test_some_unhandledError_-id-", "RuntimeException", 200);
	}

	@Test
	public void metricsGatheredWhenHandledError() throws Exception {
		mvc.perform(get("/test/some/error/10")).andExpect(status().is4xxClientError());
		assertTimer("test_some_error_-id-", null, 422);
	}
	
	@Test
	public void metricsGatheredOnRequestMappingWithRegex() throws Exception {
		mvc.perform(get("/test/some/regex/.aa")).andExpect(status().isOk());
		assertTimer("test_some_regex_-id-", null, 200);
	}

	protected void assertTimer(String uriTag, String exceptionType, Integer status) {
		MonitorConfig.Builder builder = new MonitorConfig.Builder("metricName")
				.withTag("method", "GET").withTag("uri", uriTag)
				.withTag("status", status.toString());

		if (exceptionType != null)
			builder = builder.withTag("exception", exceptionType);

		BasicTimer timer = servoMonitorCache.getTimer(builder.build());
		Assert.assertEquals(1L, (long) timer.getCount());
	}
}

@Configuration
@EnableWebMvc
@ImportAutoConfiguration({ ServoMetricsAutoConfiguration.class,
		PropertyPlaceholderAutoConfiguration.class })
class MetricsTestConfig {
	@Bean
	MetricsTestController testController() {
		return new MetricsTestController();
	}

	@Bean
	@Primary
	public MonitorRegistry monitorRegistry() {
		return new SimpleMonitorRegistry();
	}
}

@RestController
@RequestMapping("/test/some")
@ControllerAdvice
class MetricsTestController {
	@RequestMapping("/request/{id}")
	public String testSomeRequest(@PathVariable Long id) {
		return id.toString();
	}

	@RequestMapping("/error/{id}")
	public String testSomeHandledError(@PathVariable Long id) {
		throw new IllegalStateException("Boom on $id!");
	}

	@RequestMapping("/unhandledError/{id}")
	public String testSomeUnhandledError(@PathVariable Long id) {
		throw new RuntimeException("Boom on $id!");
	}
	
	@RequestMapping("/regex/{id:\\.[a-z]+}")
	public String testSomeRegexRequest(@PathVariable String id) {
		return id;
	}
	
	@ExceptionHandler(value = IllegalStateException.class)
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	ModelAndView defaultErrorHandler(HttpServletRequest request, Exception e) {
		return new ModelAndView("error");
	}
}