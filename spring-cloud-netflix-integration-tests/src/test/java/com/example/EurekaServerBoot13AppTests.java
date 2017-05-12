/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.example;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = EurekaServerBoot13App.class)
@WebIntegrationTest({"server.port=0", "turbine.enabled=false"})
@DirtiesContext
public class EurekaServerBoot13AppTests {

	@Autowired(required = false)
	@Qualifier("jerseyFilterRegistration")
	private Object jerseyFilterRegistration;

	@Autowired(required = false)
	@Qualifier("traceFilterRegistration")
	private Object traceFilterRegistration;

	@Before
	public void init() {
		//TurbineInit.stop();
	}

	@Test
	public void contextLoads() {
		assertNotNull("eureka server jersey filter was null", jerseyFilterRegistration);
		assertNotNull("trace filter was null", traceFilterRegistration);

		assertTrue("eureka server jersey filter was wrong type", jerseyFilterRegistration instanceof FilterRegistrationBean);
		assertTrue("trace filter was wrong type", traceFilterRegistration instanceof FilterRegistrationBean);
		assertTrue("wrong boot version", SpringBootVersion.getVersion().startsWith("1.3."));
	}

}
