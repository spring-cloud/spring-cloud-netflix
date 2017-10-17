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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.commons.util.UtilAutoConfiguration;
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ConfigurationBasedServerList;
import com.netflix.loadbalancer.DummyPing;
import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.PingConstant;
import com.netflix.loadbalancer.RandomRule;
import com.netflix.loadbalancer.RoundRobinRule;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.loadbalancer.ServerListFilter;
import com.netflix.loadbalancer.ZoneAwareLoadBalancer;

/**
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = RibbonClientPreprocessorOverridesIntegrationTests.TestConfiguration.class)
@DirtiesContext
public class RibbonClientPreprocessorOverridesIntegrationTests {

	@Autowired
	private SpringClientFactory factory;

	@Test
	public void ruleOverridesToRandom() throws Exception {
		RandomRule.class.cast(getLoadBalancer("foo").getRule());
		RoundRobinRule.class.cast(getLoadBalancer("bar").getRule());
	}

	@Test
	public void pingOverridesToDummy() throws Exception {
		DummyPing.class.cast(getLoadBalancer("foo").getPing());
		PingConstant.class.cast(getLoadBalancer("bar").getPing());
	}

	@Test
	public void serverListOverridesToMy() throws Exception {
		FooServiceList.class.cast(getLoadBalancer("foo").getServerListImpl());
		BarServiceList.class.cast(getLoadBalancer("bar").getServerListImpl());
	}

	@SuppressWarnings("unchecked")
	private ZoneAwareLoadBalancer<Server> getLoadBalancer(String name) {
		return (ZoneAwareLoadBalancer<Server>) this.factory.getLoadBalancer(name);
	}

	@Test
	public void serverListFilterOverride() throws Exception {
		ServerListFilter<Server> filter = getLoadBalancer("foo").getFilter();
		assertEquals("FooTestZone",
				ZonePreferenceServerListFilter.class.cast(filter)
						.getZone());
	}

	@Configuration
	@RibbonClients({
		@RibbonClient(name = "foo", configuration = FooConfiguration.class),
		@RibbonClient(name = "bar", configuration = BarConfiguration.class)
	})
	@Import({ UtilAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class,
			ArchaiusAutoConfiguration.class, RibbonAutoConfiguration.class})
	protected static class TestConfiguration {
	}

	@Configuration
	public static class FooConfiguration {
		@Bean
		public IRule ribbonRule() {
			return new RandomRule();
		}

		@Bean
		public IPing ribbonPing() {
			return new DummyPing();
		}

		@Bean
		public ServerList<Server> ribbonServerList(IClientConfig config) {
			return new FooServiceList(config);
		}

		@Bean
		public ZonePreferenceServerListFilter serverListFilter() {
			ZonePreferenceServerListFilter filter = new ZonePreferenceServerListFilter();
			filter.setZone("FooTestZone");
			return filter;
		}
	}

	public static class FooServiceList extends ConfigurationBasedServerList {
		public FooServiceList(IClientConfig config) {
			super.initWithNiwsConfig(config);
		}
	}

	@Configuration
	public static class BarConfiguration {

		@Bean
		public IRule ribbonRule() {
			return new RoundRobinRule();
		}

		@Bean
		public IPing ribbonPing() {
			return new PingConstant();
		}

		@Bean
		public ServerList<Server> ribbonServerList(IClientConfig config) {
			return new BarServiceList(config);
		}

		@Bean
		public ZonePreferenceServerListFilter serverListFilter() {
			ZonePreferenceServerListFilter filter = new ZonePreferenceServerListFilter();
			filter.setZone("BarTestZone");
			return filter;
		}
	}

	public static class BarServiceList extends ConfigurationBasedServerList {
		public BarServiceList(IClientConfig config) {
			super.initWithNiwsConfig(config);
		}
	}
}
