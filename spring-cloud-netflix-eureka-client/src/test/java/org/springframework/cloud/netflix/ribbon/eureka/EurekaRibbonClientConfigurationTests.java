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

package org.springframework.cloud.netflix.ribbon.eureka;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;

import com.netflix.config.ConfigurationManager;
import com.netflix.config.DeploymentContext.ContextKey;
import com.netflix.config.DynamicStringProperty;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.ZoneAwareLoadBalancer;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.cloud.netflix.ribbon.RibbonUtils.VALUE_NOT_SET;
import static org.springframework.cloud.netflix.ribbon.RibbonUtils.getProperty;
import static org.springframework.cloud.netflix.ribbon.RibbonUtils.getRibbonKey;
import static org.springframework.cloud.netflix.ribbon.RibbonUtils.setRibbonProperty;

/**
 * @author Dave Syer
 * @author Ryan Baxter
 */
public class EurekaRibbonClientConfigurationTests {

	@After
	@Before
	public void close() {
		ConfigurationManager.getDeploymentContext().setValue(ContextKey.zone, "");
	}

	@Test
	@Ignore
	public void basicConfigurationCreatedForLoadBalancer() {
		EurekaClientConfigBean client = new EurekaClientConfigBean();
		EurekaInstanceConfigBean configBean = getEurekaInstanceConfigBean();
		client.getAvailabilityZones().put(client.getRegion(), "foo");
		SpringClientFactory clientFactory = new SpringClientFactory();
		EurekaRibbonClientConfiguration clientPreprocessor = new EurekaRibbonClientConfiguration(
				client, "service", configBean, false);
		clientPreprocessor.preprocess();
		ILoadBalancer balancer = clientFactory.getLoadBalancer("service");
		assertNotNull(balancer);
		@SuppressWarnings("unchecked")
		ZoneAwareLoadBalancer<DiscoveryEnabledServer> aware = (ZoneAwareLoadBalancer<DiscoveryEnabledServer>) balancer;
		assertTrue(aware.getServerListImpl() instanceof DomainExtractingServerList);
		assertEquals("foo",
				ConfigurationManager.getDeploymentContext().getValue(ContextKey.zone));
	}

	private EurekaInstanceConfigBean getEurekaInstanceConfigBean() {
		return new EurekaInstanceConfigBean(new InetUtils(new InetUtilsProperties()));
	}

	@Test
	public void testSetProp() {
		EurekaClientConfigBean client = new EurekaClientConfigBean();
		EurekaInstanceConfigBean configBean = getEurekaInstanceConfigBean();
		EurekaRibbonClientConfiguration preprocessor = new EurekaRibbonClientConfiguration(
				client, "myService", configBean, false);
		String serviceId = "myService";
		String suffix = "mySuffix";
		String value = "myValue";
		DynamicStringProperty property = getProperty(getRibbonKey(serviceId, suffix));
		assertEquals("property doesn't have default value", VALUE_NOT_SET,
				property.get());
		setRibbonProperty(serviceId, suffix, value);
		assertEquals("property has wrong value", value, property.get());
		setRibbonProperty(serviceId, suffix, value);
		assertEquals("property has wrong value", value, property.get());
	}

	@Test
	public void testExplicitZone() {
		EurekaClientConfigBean client = new EurekaClientConfigBean();
		EurekaInstanceConfigBean configBean = getEurekaInstanceConfigBean();
		configBean.getMetadataMap().put("zone", "myZone");
		EurekaRibbonClientConfiguration preprocessor = new EurekaRibbonClientConfiguration(
				client, "myService", configBean, false);
		preprocessor.preprocess();
		assertEquals("myZone",
				ConfigurationManager.getDeploymentContext().getValue(ContextKey.zone));
	}

	@Test
	public void testDefaultZone() {
		EurekaClientConfigBean client = new EurekaClientConfigBean();
		EurekaInstanceConfigBean configBean = getEurekaInstanceConfigBean();
		EurekaRibbonClientConfiguration preprocessor = new EurekaRibbonClientConfiguration(
				client, "myService", configBean, false);
		preprocessor.preprocess();
		assertEquals("defaultZone",
				ConfigurationManager.getDeploymentContext().getValue(ContextKey.zone));
	}

	@Test
	public void testApproximateZone() {
		EurekaClientConfigBean client = new EurekaClientConfigBean();
		EurekaInstanceConfigBean configBean = getEurekaInstanceConfigBean();
		configBean.setHostname("this.is.a.test.com");
		EurekaRibbonClientConfiguration preprocessor = new EurekaRibbonClientConfiguration(
				client, "myService", configBean, true);
		preprocessor.preprocess();
		assertEquals("is.a.test.com",
				ConfigurationManager.getDeploymentContext().getValue(ContextKey.zone));
	}

}
