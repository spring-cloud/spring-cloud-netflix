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

package org.springframework.cloud.netflix.ribbon;

import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.PollingServerListUpdater;
import com.netflix.loadbalancer.ServerListUpdater;
import com.netflix.loadbalancer.ZoneAwareLoadBalancer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

/**
 * @author Dave Syer
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = RibbonClientConfigurationIntegrationTests.TestLBConfig.class,
		properties = "test.ribbon.ServerListRefreshInterval=999")
@DirtiesContext
public class RibbonClientConfigurationIntegrationTests {

	@Autowired
	private SpringClientFactory clientFactory;

	@Test
	public void testLoadBalancerConstruction() {
		ILoadBalancer loadBalancer = clientFactory.getInstance("test", ILoadBalancer.class);
		assertThat(loadBalancer, is(instanceOf(ZoneAwareLoadBalancer.class)));
		ZoneAwareLoadBalancer lb = (ZoneAwareLoadBalancer) loadBalancer;
		ServerListUpdater serverListUpdater = (PollingServerListUpdater) ReflectionTestUtils.getField(loadBalancer, "serverListUpdater");
		Long refreshIntervalMs = (Long) ReflectionTestUtils.getField(serverListUpdater, "refreshIntervalMs");
		// assertThat(refreshIntervalMs, equalTo(999L));

		ServerListUpdater updater = clientFactory.getInstance("test", ServerListUpdater.class);
		assertThat(updater, is(sameInstance(serverListUpdater)));
	}

	@Configuration
	@EnableAutoConfiguration
	protected static class TestLBConfig { }

}
