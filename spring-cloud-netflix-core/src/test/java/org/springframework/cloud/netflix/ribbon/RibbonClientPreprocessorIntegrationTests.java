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
 */

package org.springframework.cloud.netflix.ribbon;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonClientPreprocessorIntegrationTests.PlainConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.netflix.loadbalancer.ConfigurationBasedServerList;
import com.netflix.loadbalancer.DummyPing;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ZoneAvoidanceRule;
import com.netflix.loadbalancer.ZoneAwareLoadBalancer;

/**
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = PlainConfiguration.class)
@DirtiesContext
public class RibbonClientPreprocessorIntegrationTests {

	@Autowired
	private SpringClientFactory factory;

	@Test
	public void ruleDefaultsToZoneAvoidance() throws Exception {
		ZoneAvoidanceRule.class.cast(getLoadBalancer().getRule());
	}

	@Test
	public void serverListFilterDefaultsToZonePreference() throws Exception {
		ZonePreferenceServerListFilter.class.cast(getLoadBalancer().getFilter());
	}

	@Test
	public void pingDefaultsToDummy() throws Exception {
		DummyPing.class.cast(getLoadBalancer().getPing());
	}

	@Test
	public void serverListDefaultsToConfigurationBased() throws Exception {
		ConfigurationBasedServerList.class.cast(getLoadBalancer().getServerListImpl());
	}

	@SuppressWarnings("unchecked")
	private ZoneAwareLoadBalancer<Server> getLoadBalancer() {
		return (ZoneAwareLoadBalancer<Server>) this.factory.getLoadBalancer("foo");
	}

	@Configuration
	@RibbonClient(name = "foo")
	@Import({ PropertyPlaceholderAutoConfiguration.class,
			ArchaiusAutoConfiguration.class, RibbonAutoConfiguration.class })
	protected static class PlainConfiguration {
	}

}
