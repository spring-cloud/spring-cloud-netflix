/*
 * Copyright 2013-2014 the original author or authors.
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
 */

package org.springframework.cloud.netflix.ribbon;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonClientsPreprocessorIntegrationTests.TestConfiguration;
import org.springframework.cloud.netflix.ribbon.eureka.DomainExtractingServerList;
import org.springframework.cloud.netflix.ribbon.eureka.RibbonEurekaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ZoneAvoidanceRule;
import com.netflix.loadbalancer.ZoneAwareLoadBalancer;

import static org.junit.Assert.assertEquals;

/**
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestConfiguration.class)
@DirtiesContext
public class RibbonClientsPreprocessorIntegrationTests {

	@Autowired
	private SpringClientFactory factory;

	@Test
	public void serverListIsWrapped() throws Exception {
		@SuppressWarnings("unchecked")
		ZoneAwareLoadBalancer<Server> loadBalancer = (ZoneAwareLoadBalancer<Server>) this.factory
				.getLoadBalancer("foo");
		DomainExtractingServerList.class.cast(loadBalancer.getServerListImpl());
	}

	@Test
	public void ruleDefaultsToZoneAvoidance() throws Exception {
		@SuppressWarnings("unchecked")
		ZoneAwareLoadBalancer<Server> loadBalancer = (ZoneAwareLoadBalancer<Server>) this.factory
				.getLoadBalancer("foo");
		ZoneAvoidanceRule.class.cast(loadBalancer.getRule());
	}

	@Test
	public void serverListFilterOverride() throws Exception {
		@SuppressWarnings("unchecked")
		ZoneAwareLoadBalancer<Server> loadBalancer = (ZoneAwareLoadBalancer<Server>) this.factory
				.getLoadBalancer("foo");
		assertEquals("myTestZone",
				ZonePreferenceServerListFilter.class.cast(loadBalancer.getFilter())
						.getZone());
	}

	@Configuration
	@RibbonClients(@RibbonClient(name = "foo", configuration = FooConfiguration.class))
	@Import({ PropertyPlaceholderAutoConfiguration.class,
			ArchaiusAutoConfiguration.class, EurekaClientAutoConfiguration.class,
			RibbonAutoConfiguration.class, RibbonEurekaAutoConfiguration.class })
	protected static class TestConfiguration {
	}

	@Configuration
	protected static class FooConfiguration {
		@Bean
		public ZonePreferenceServerListFilter serverListFilter() {
			ZonePreferenceServerListFilter filter = new ZonePreferenceServerListFilter();
			filter.setZone("myTestZone");
			return filter;
		}
	}

}
