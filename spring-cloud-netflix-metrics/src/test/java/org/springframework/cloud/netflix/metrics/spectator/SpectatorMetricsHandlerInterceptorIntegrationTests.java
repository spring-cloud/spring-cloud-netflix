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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.test.ImportAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.netflix.spectator.api.Registry;

/**
 * @author Jon Schneider
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpectatorTestConfig.class)
@WebAppConfiguration
@TestPropertySource(properties = "netflix.spectator.rest.metricName=metricName")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SpectatorMetricsHandlerInterceptorIntegrationTests {
	@Autowired
	WebApplicationContext webAppContext;

	@Autowired
	Registry registry;

	MockMvc mvc;

	@Test
	public void autoConfigurationWiresTheMetricsInterceptor() {
		assertFalse(webAppContext.getBeansOfType(SpectatorHandlerInterceptor.class).isEmpty());
	}

	@Before
	public void setup() {
		mvc = MockMvcBuilders.webAppContextSetup(webAppContext).build();
	}

	@Test
	public void metricsGatheredWhenSuccess() throws Exception {
		mvc.perform(get("/test/some/request/10")).andExpect(status().isOk());
		assertTimer("test_some_request_-id-", "none", 200);
	}

	@Test
	public void metricsGatheredWhenClientRequestBad() throws Exception {
		mvc.perform(get("/test/some/request/oops")).andExpect(status().is4xxClientError());
		assertTimer("test_some_request_-id-", "none", 400);
	}

	@Test
	public void metricsGatheredWhenUnhandledError() throws Exception {
		try {
			mvc.perform(get("/test/some/unhandledError/10")).andExpect(status().isOk());
		} catch (Exception e) {
		}
		assertTimer("test_some_unhandledError_-id-", "RuntimeException", 200);
	}

	@Test
	public void metricsGatheredWhenHandledError() throws Exception {
		mvc.perform(get("/test/some/error/10")).andExpect(status().is4xxClientError());
		assertTimer("test_some_error_-id-", "none", 422);
	}

	protected void assertTimer(String uriTag, String exceptionType, Integer status) {
		assertEquals(
				1,
				registry.timer("metricName", "method", "GET", "uri", uriTag, "caller", "unknown", "exceptionType",
						exceptionType, "status", status.toString()).count());
	}
}

@Configuration
@EnableWebMvc
@ImportAutoConfiguration({ SpectatorAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
class SpectatorTestConfig {
	@Bean
	SpectatorTestController testController() {
		return new SpectatorTestController();
	}
}

@RestController
@RequestMapping("/test/some")
@ControllerAdvice
class SpectatorTestController {
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

	@ExceptionHandler(value = IllegalStateException.class)
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	ModelAndView defaultErrorHandler(HttpServletRequest request, Exception e) {
		return new ModelAndView("error");
	}
}