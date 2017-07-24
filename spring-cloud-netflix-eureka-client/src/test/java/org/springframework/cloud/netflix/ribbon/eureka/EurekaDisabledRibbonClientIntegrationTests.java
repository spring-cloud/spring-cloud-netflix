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
 */

package org.springframework.cloud.netflix.ribbon.eureka;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ZoneAvoidanceRule;
import com.netflix.loadbalancer.ZoneAwareLoadBalancer;
import com.netflix.niws.loadbalancer.NIWSDiscoveryPing;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.commons.util.UtilAutoConfiguration;
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClientConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.ribbon.eureka.EurekaRibbonClientPreprocessorIntegrationTests.TestConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ribbon Eureka client should be disabled if Eureka client is not enabled
 * 
 * @author Biju Kunjummen
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TestConfiguration.class, properties = "eureka.client.enabled=false")
@DirtiesContext
public class EurekaDisabledRibbonClientIntegrationTests {

	@Autowired
	private SpringClientFactory factory;

	@Test
	public void serverListShouldNotBeEurekaBased() throws Exception {
		assertThat(getLoadBalancer().getServerListImpl())
				.isNotInstanceOf(DomainExtractingServerList.class);
	}

	@Test
	public void ruleDefaultsToZoneAvoidance() throws Exception {
		ZoneAvoidanceRule.class.cast(getLoadBalancer().getRule());
	}

	@Test
	public void pingShouldNotBeEurekaBased() throws Exception {
		assertThat(getLoadBalancer().getPing()).isNotInstanceOf(NIWSDiscoveryPing.class);
	}

	@Test
	public void serverIntrospectorShouldNotBeEurekaBased() throws Exception {
		assertThat(this.factory.getInstance("foo", ServerIntrospector.class))
				.isNotInstanceOf(EurekaServerIntrospector.class);
	}

	@SuppressWarnings("unchecked")
	private ZoneAwareLoadBalancer<Server> getLoadBalancer() {
		return (ZoneAwareLoadBalancer<Server>) this.factory.getLoadBalancer("foo");
	}

	@Configuration
	@RibbonClient("foo")
	@Import({ UtilAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class,
			ArchaiusAutoConfiguration.class, RibbonAutoConfiguration.class,
			EurekaDiscoveryClientConfiguration.class, EurekaClientAutoConfiguration.class,
			RibbonEurekaAutoConfiguration.class })
	protected static class TestConfiguration {

	}

}
