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

import static org.junit.Assert.assertEquals;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = RibbonClientPreprocessorOverridesIntegrationTests.TestConfiguration.class)
@DirtiesContext
public class RibbonClientPreprocessorOverridesIntegrationTests {

	@Autowired
	private SpringClientFactory factory;

	@Test
	public void ruleOverridesToRandom() throws Exception {
		RandomRule.class.cast(getLoadBalancer().getRule());
	}

	@Test
	public void pingOverridesToDummy() throws Exception {
		DummyPing.class.cast(getLoadBalancer().getPing());
	}

	@Test
	public void serverListOverridesToMy() throws Exception {
		MyServiceList.class.cast(getLoadBalancer().getServerListImpl());
	}

	@SuppressWarnings("unchecked")
	private ZoneAwareLoadBalancer<Server> getLoadBalancer() {
		return (ZoneAwareLoadBalancer<Server>) this.factory
				.getLoadBalancer("foo");
	}

	@Test
	public void serverListFilterOverride() throws Exception {
		ServerListFilter<Server> filter = getLoadBalancer().getFilter();
		assertEquals("MyTestZone",
				ZonePreferenceServerListFilter.class.cast(filter)
						.getZone());
	}

	@Configuration
	@RibbonClient(name = "foo", configuration = FooConfiguration.class)
	@Import({ PropertyPlaceholderAutoConfiguration.class,
			ArchaiusAutoConfiguration.class, EurekaClientAutoConfiguration.class,
			RibbonAutoConfiguration.class})
	protected static class TestConfiguration {
	}

	@Configuration
	public static class FooConfiguration {

		public FooConfiguration() {
			System.out.println("here");
		}

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
			return new MyServiceList(config);
		}

		@Bean
		public ZonePreferenceServerListFilter serverListFilter() {
			ZonePreferenceServerListFilter filter = new ZonePreferenceServerListFilter();
			filter.setZone("MyTestZone");
			return filter;
		}
	}

	public static class MyServiceList extends ConfigurationBasedServerList {
		public MyServiceList(IClientConfig config) {
			super.initWithNiwsConfig(config);
		}
	}

}
