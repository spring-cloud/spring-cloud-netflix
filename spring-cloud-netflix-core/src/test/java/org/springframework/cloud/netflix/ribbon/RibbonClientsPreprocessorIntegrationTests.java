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

import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.PingUrl;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ZoneAvoidanceRule;
import com.netflix.loadbalancer.ZoneAwareLoadBalancer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.commons.httpclient.HttpClientConfiguration;
import org.springframework.cloud.commons.util.UtilAutoConfiguration;
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonClientsPreprocessorIntegrationTests.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
@DirtiesContext
public class RibbonClientsPreprocessorIntegrationTests {

	@Autowired
	private SpringClientFactory factory;

	@Test
	public void ruleDefaultsToZoneAvoidance() throws Exception {
		ZoneAvoidanceRule.class.cast(getLoadBalancer().getRule());
	}

	@SuppressWarnings("unchecked")
	private ZoneAwareLoadBalancer<Server> getLoadBalancer() {
		return (ZoneAwareLoadBalancer<Server>) this.factory.getLoadBalancer("foo");
	}

	@Test
	public void serverListFilterOverride() throws Exception {
		assertThat(ZonePreferenceServerListFilter.class
				.cast(getLoadBalancer().getFilter()).getZone()).isEqualTo("myTestZone");
	}

	@Test
	public void pingOverride() throws Exception {
		assertThat(getLoadBalancer().getPing()).isInstanceOf(PingUrl.class);
	}

	@Configuration
	@RibbonClients(@RibbonClient(name = "foo", configuration = FooConfiguration.class))
	@Import({ UtilAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class,
			ArchaiusAutoConfiguration.class, RibbonAutoConfiguration.class, HttpClientConfiguration.class})
	protected static class TestConfiguration {
	}

	// tag::sample_override_ribbon_config[]
	@Configuration
	protected static class FooConfiguration {
		@Bean
		public ZonePreferenceServerListFilter serverListFilter() {
			ZonePreferenceServerListFilter filter = new ZonePreferenceServerListFilter();
			filter.setZone("myTestZone");
			return filter;
		}

		@Bean
		public IPing ribbonPing() {
			return new PingUrl();
		}
	}
	// end::sample_override_ribbon_config[]

}
