/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.netflix.zuul;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.zuul.filters.CompositeRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonRoutingFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * To test the auto-configuration of Zuul Proxy
 * 
 * @author Biju Kunjummen
 * 
 */

@RunWith(SpringRunner.class)
@SpringBootTest
public class ZuulServerAutoConfigurationTests {
	
	@Autowired
	private RouteLocator routeLocator;

	@Autowired(required = false)
	private RibbonRoutingFilter ribbonRoutingFilter;
	
	@Test
	public void testAutoConfiguredBeans() {
		assertThat(routeLocator).isInstanceOf(CompositeRouteLocator.class);
		assertThat(ribbonRoutingFilter).isNull();
	}
	
	
	@Configuration
	@EnableAutoConfiguration
	@EnableZuulServer
	static class TestConfig {
	}	
}
