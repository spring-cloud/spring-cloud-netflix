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

package org.springframework.cloud.netflix.ribbon.eureka;

import com.netflix.config.ConfigurationManager;
import com.netflix.config.DeploymentContext.ContextKey;
import com.netflix.config.DynamicStringProperty;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.ZoneAwareLoadBalancer;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;

import static org.assertj.core.api.Assertions.assertThat;
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
		assertThat(balancer).isNotNull();
		@SuppressWarnings("unchecked")
		ZoneAwareLoadBalancer<DiscoveryEnabledServer> aware = (ZoneAwareLoadBalancer<DiscoveryEnabledServer>) balancer;
		assertThat(aware.getServerListImpl() instanceof DomainExtractingServerList)
				.isTrue();
		assertThat(ConfigurationManager.getDeploymentContext().getValue(ContextKey.zone))
				.isEqualTo("foo");
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
		assertThat(property.get()).as("property doesn't have default value")
				.isEqualTo(VALUE_NOT_SET);
		setRibbonProperty(serviceId, suffix, value);
		assertThat(property.get()).as("property has wrong value").isEqualTo(value);
		setRibbonProperty(serviceId, suffix, value);
		assertThat(property.get()).as("property has wrong value").isEqualTo(value);
	}

	@Test
	public void testExplicitZone() {
		EurekaClientConfigBean client = new EurekaClientConfigBean();
		EurekaInstanceConfigBean configBean = getEurekaInstanceConfigBean();
		configBean.getMetadataMap().put("zone", "myZone");
		EurekaRibbonClientConfiguration preprocessor = new EurekaRibbonClientConfiguration(
				client, "myService", configBean, false);
		preprocessor.preprocess();
		assertThat(ConfigurationManager.getDeploymentContext().getValue(ContextKey.zone))
				.isEqualTo("myZone");
	}

	@Test
	public void testDefaultZone() {
		EurekaClientConfigBean client = new EurekaClientConfigBean();
		EurekaInstanceConfigBean configBean = getEurekaInstanceConfigBean();
		EurekaRibbonClientConfiguration preprocessor = new EurekaRibbonClientConfiguration(
				client, "myService", configBean, false);
		preprocessor.preprocess();
		assertThat(ConfigurationManager.getDeploymentContext().getValue(ContextKey.zone))
				.isEqualTo("defaultZone");
	}

	@Test
	public void testApproximateZone() {
		EurekaClientConfigBean client = new EurekaClientConfigBean();
		EurekaInstanceConfigBean configBean = getEurekaInstanceConfigBean();
		configBean.setHostname("this.is.a.test.com");
		EurekaRibbonClientConfiguration preprocessor = new EurekaRibbonClientConfiguration(
				client, "myService", configBean, true);
		preprocessor.preprocess();
		assertThat(ConfigurationManager.getDeploymentContext().getValue(ContextKey.zone))
				.isEqualTo("is.a.test.com");
	}

}
