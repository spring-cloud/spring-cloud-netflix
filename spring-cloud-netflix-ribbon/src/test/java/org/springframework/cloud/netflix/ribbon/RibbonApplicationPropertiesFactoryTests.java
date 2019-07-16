/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.netflix.ribbon;

import com.netflix.loadbalancer.ILoadBalancer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootConfiguration
@SpringBootTest(classes = RibbonApplicationPropertiesFactoryTests.TestConfig.class)
public class RibbonApplicationPropertiesFactoryTests {

	@Autowired
	PropertiesFactory propertiesFactory;

	@Test
	public void testGetClassName() {
		String factoryClassName = propertiesFactory.getClassName(ILoadBalancer.class,
				"test");
		Assert.assertEquals("testProperty", factoryClassName);
	}

	@Configuration
	@EnableAutoConfiguration
	protected static class TestConfig {

		@Primary
		@Bean
		public Environment getMockEnvironment() {
			MockEnvironment mockEnvironment = new MockEnvironment();
			mockEnvironment.setProperty("ribbon.NFLoadBalancerClassName", "testProperty");
			return mockEnvironment;
		}

	}

}
