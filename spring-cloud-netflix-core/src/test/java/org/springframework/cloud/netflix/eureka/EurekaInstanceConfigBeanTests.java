/*
 Copyright 2013-2014 the original author or authors.
 *
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 *
      http://www.apache.org/licenses/LICENSE-2.0
 *
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package org.springframework.cloud.netflix.eureka;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.appinfo.UniqueIdentifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.EnvironmentTestUtils.addEnvironment;

/**
 * @author Dave Syer
 */
public class EurekaInstanceConfigBeanTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@After
	public void init() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void idFromInstanceId() throws Exception {
		EurekaInstanceConfigBean instance = new EurekaInstanceConfigBean();
		instance.getMetadataMap().put("instanceId", "foo");
		instance.setHostname("bar");
		assertEquals("bar:foo", ((UniqueIdentifier) instance.getDataCenterInfo()).getId());
	}

	@Test
	public void basicBinding() {
		addEnvironment(this.context, "eureka.instance.appGroupName=mygroup");
		setupContext();
		assertEquals("mygroup", getInstanceConfig().getAppGroupName());
	}

	@Test
	public void nonSecurePort() {
		addEnvironment(this.context, "eureka.instance.nonSecurePort:8888");
		setupContext();
		assertEquals(8888, getInstanceConfig().getNonSecurePort());
	}

	@Test
	public void testDefaultInitialStatus() {
		setupContext();
		assertEquals("initialStatus wrong", InstanceStatus.UP, getInstanceConfig()
				.getInitialStatus());
	}

	@Test(expected = BeanCreationException.class)
	public void testBadInitialStatus() {
		addEnvironment(this.context, "eureka.instance.initial-status:FOO");
		setupContext();
	}

	@Test
	public void testCustomInitialStatus() {
		addEnvironment(this.context, "eureka.instance.initial-status:STARTING");
		setupContext();
		assertEquals("initialStatus wrong", InstanceStatus.STARTING, getInstanceConfig()
				.getInitialStatus());
	}

	@Test
	public void testPerferIpAddress() throws Exception {
		addEnvironment(this.context, "eureka.instance.preferIpAddress:true");
		setupContext();
		EurekaInstanceConfigBean instance = getInstanceConfig();
		assertTrue("Wrong hostname: " + instance.getHostname(), getInstanceConfig()
				.getHostname().equals(instance.getIpAddress()));

	}

	@Test
	public void testPerferIpAddressInDatacenter() throws Exception {
		addEnvironment(this.context, "eureka.instance.preferIpAddress:true");
		setupContext();
		EurekaInstanceConfigBean instance = getInstanceConfig();
		String id = ((UniqueIdentifier) instance.getDataCenterInfo()).getId();
		assertTrue("Wrong hostname: " + id, id.equals(instance.getIpAddress()));

	}

	private void setupContext() {
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				TestConfiguration.class);
		this.context.refresh();
	}

	private EurekaInstanceConfigBean getInstanceConfig() {
		return this.context.getBean(EurekaInstanceConfigBean.class);
	}

	@Configuration
	@EnableConfigurationProperties(EurekaInstanceConfigBean.class)
	protected static class TestConfiguration {

	}

}
