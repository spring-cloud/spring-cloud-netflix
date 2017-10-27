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
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfigurationIntegrationTests.TestConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;

import static org.junit.Assert.assertEquals;

/**
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TestConfiguration.class, value = {"ribbon.ConnectTimeout=25000"})
@DirtiesContext
public class RibbonAutoConfigurationIntegrationTests {

	@Autowired
	private SpringClientFactory factory;

	@Test
	public void serverListIsConfigured() throws Exception {
		IClientConfig config = this.factory.getClientConfig("client");
		assertEquals(25000,
				config.getPropertyAsInteger(CommonClientConfigKey.ConnectTimeout, 3000));
	}

	@Configuration
	@RibbonClient("client")
	@Import({ PropertyPlaceholderAutoConfiguration.class, ArchaiusAutoConfiguration.class,
			RibbonAutoConfiguration.class })
	protected static class TestConfiguration {
	}

}
