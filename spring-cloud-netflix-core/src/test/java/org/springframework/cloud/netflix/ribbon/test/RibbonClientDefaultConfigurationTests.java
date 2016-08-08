/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.cloud.netflix.ribbon.test;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.ribbon.test.RibbonClientDefaultConfigurationTestsConfig.BazServiceList;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.netflix.loadbalancer.BestAvailableRule;
import com.netflix.loadbalancer.PingUrl;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerListSubsetFilter;
import com.netflix.loadbalancer.ZoneAwareLoadBalancer;

/**
 * @author Dave Syer
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = RibbonClientDefaultConfigurationTestsConfig.class, value = "ribbon.eureka.enabled=true")
@DirtiesContext
public class RibbonClientDefaultConfigurationTests {

	@Autowired
	private SpringClientFactory factory;

	@Test
	public void ruleOverridesDefault() throws Exception {
		assertThat("wrong rule type", getLoadBalancer("baz").getRule(),
				is(instanceOf(BestAvailableRule.class)));
	}

	@Test
	public void pingOverridesDefault() throws Exception {
		assertThat("wrong ping type", getLoadBalancer("baz").getPing(),
				is(instanceOf(PingUrl.class)));
	}

	@Test
	public void serverListOverridesDefault() throws Exception {
		assertThat("wrong server list type", getLoadBalancer("baz").getServerListImpl(),
				is(instanceOf(BazServiceList.class)));
	}

	@SuppressWarnings("unchecked")
	private ZoneAwareLoadBalancer<Server> getLoadBalancer(String name) {
		return (ZoneAwareLoadBalancer<Server>) this.factory.getLoadBalancer(name);
	}

	@Test
	public void serverListFilterOverride() throws Exception {
		assertThat("wrong filter type", getLoadBalancer("baz").getFilter(),
				is(instanceOf(ServerListSubsetFilter.class)));
	}

}
