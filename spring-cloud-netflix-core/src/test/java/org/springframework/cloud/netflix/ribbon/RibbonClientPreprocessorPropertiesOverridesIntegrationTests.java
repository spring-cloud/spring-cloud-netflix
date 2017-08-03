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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assume.assumeThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.commons.httpclient.HttpClientConfiguration;
import org.springframework.cloud.commons.util.UtilAutoConfiguration;
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration;
import org.springframework.cloud.netflix.ribbon.test.TestLoadBalancer;
import org.springframework.cloud.netflix.ribbon.test.TestServerList;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.netflix.loadbalancer.ConfigurationBasedServerList;
import com.netflix.loadbalancer.DummyPing;
import com.netflix.loadbalancer.NoOpPing;
import com.netflix.loadbalancer.RandomRule;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerListSubsetFilter;
import com.netflix.loadbalancer.ZoneAvoidanceRule;
import com.netflix.loadbalancer.ZoneAwareLoadBalancer;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = RibbonClientPreprocessorPropertiesOverridesIntegrationTests.TestConfiguration.class)
@DirtiesContext
public class RibbonClientPreprocessorPropertiesOverridesIntegrationTests {

	@Autowired
	private SpringClientFactory factory;

	@Test
	public void ruleOverridesToRandom() throws Exception {
		assumeNotTravis();
		RandomRule.class.cast(getLoadBalancer("foo2").getRule());
		ZoneAvoidanceRule.class.cast(getLoadBalancer("bar").getRule());
	}

	// TODO: why do these tests fail in travis?
	void assumeNotTravis() {
		assumeThat("running in travis, skipping", System.getenv("TRAVIS"),
				is(not(equalTo("true"))));
	}

	@Test
	public void pingOverridesToNoOp() throws Exception {
		NoOpPing.class.cast(getLoadBalancer("foo2").getPing());
		DummyPing.class.cast(getLoadBalancer("bar").getPing());
	}

	@Test
	public void serverListOverridesToTest() throws Exception {
		assumeNotTravis();
		TestServerList.class.cast(getLoadBalancer("foo2").getServerListImpl());
		ConfigurationBasedServerList.class
				.cast(getLoadBalancer("bar").getServerListImpl());
	}

	@Test
	public void loadBalancerOverridesToTest() throws Exception {
		TestLoadBalancer.class.cast(getLoadBalancer("foo2"));
		ZoneAwareLoadBalancer.class.cast(getLoadBalancer("bar"));
	}

	@Test
	public void serverListFilterOverride() throws Exception {
		assumeNotTravis();
		ServerListSubsetFilter.class.cast(getLoadBalancer("foo2").getFilter());
		ZonePreferenceServerListFilter.class.cast(getLoadBalancer("bar").getFilter());
	}

	@SuppressWarnings("unchecked")
	private ZoneAwareLoadBalancer<Server> getLoadBalancer(String name) {
		return (ZoneAwareLoadBalancer<Server>) this.factory.getLoadBalancer(name);
	}

	@Configuration
	@RibbonClients
	@Import({ UtilAutoConfiguration.class, HttpClientConfiguration.class, PropertyPlaceholderAutoConfiguration.class,
			ArchaiusAutoConfiguration.class, RibbonAutoConfiguration.class })
	protected static class TestConfiguration {
	}

}
