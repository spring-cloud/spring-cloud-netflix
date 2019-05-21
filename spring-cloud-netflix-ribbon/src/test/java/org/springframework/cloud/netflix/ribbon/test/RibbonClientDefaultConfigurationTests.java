/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.ribbon.test;

import com.netflix.loadbalancer.BestAvailableRule;
import com.netflix.loadbalancer.PingUrl;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerListSubsetFilter;
import com.netflix.loadbalancer.ZoneAwareLoadBalancer;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.ribbon.test.RibbonClientDefaultConfigurationTestsConfig.BazServiceList;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = RibbonClientDefaultConfigurationTestsConfig.class,
		value = "ribbon.eureka.enabled=true")
@DirtiesContext
public class RibbonClientDefaultConfigurationTests {

	@Autowired
	private SpringClientFactory factory;

	@Test
	public void ruleOverridesDefault() throws Exception {
		assertThat(getLoadBalancer("baz").getRule()).as("wrong rule type")
				.isInstanceOf(BestAvailableRule.class);
	}

	@Test
	public void pingOverridesDefault() throws Exception {
		assertThat(getLoadBalancer("baz").getPing()).as("wrong ping type")
				.isInstanceOf(PingUrl.class);
	}

	@Test
	public void serverListOverridesDefault() throws Exception {
		assertThat(getLoadBalancer("baz").getServerListImpl())
				.as("wrong server list type").isInstanceOf(BazServiceList.class);
	}

	@SuppressWarnings("unchecked")
	private ZoneAwareLoadBalancer<Server> getLoadBalancer(String name) {
		return (ZoneAwareLoadBalancer<Server>) this.factory.getLoadBalancer(name);
	}

	@Test
	public void serverListFilterOverride() throws Exception {
		assertThat(getLoadBalancer("baz").getFilter()).as("wrong filter type")
				.isInstanceOf(ServerListSubsetFilter.class);
	}

}
