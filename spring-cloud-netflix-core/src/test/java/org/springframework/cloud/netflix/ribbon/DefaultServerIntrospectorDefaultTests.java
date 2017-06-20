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

import com.netflix.loadbalancer.Server;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Rico Pahlisch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = DefaultServerIntrospectorDefaultTests.TestConfiguration.class)
public class DefaultServerIntrospectorDefaultTests {

	@Autowired
	private ServerIntrospector serverIntrospector;

	@Test
	public void testDefaultSslPorts(){
		Server serverMock = mock(Server.class);
		when(serverMock.getPort()).thenReturn(443);
		Assert.assertTrue(serverIntrospector.isSecure(serverMock));
		when(serverMock.getPort()).thenReturn(8443);
		Assert.assertTrue(serverIntrospector.isSecure(serverMock));

		when(serverMock.getPort()).thenReturn(16443);
		Assert.assertFalse(serverIntrospector.isSecure(serverMock));
	}

	@Configuration
	@EnableConfigurationProperties(ServerIntrospectorProperties.class)
	protected static class TestConfiguration {
		@Bean
		public DefaultServerIntrospector defaultServerIntrospector(){
			return new DefaultServerIntrospector();
		}
	}
}
