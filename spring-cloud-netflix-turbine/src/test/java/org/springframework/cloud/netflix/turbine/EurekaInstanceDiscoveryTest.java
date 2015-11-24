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

package org.springframework.cloud.netflix.turbine;

import org.junit.Before;
import org.junit.Test;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.turbine.discovery.Instance;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Spencer Gibb
 */
public class EurekaInstanceDiscoveryTest {

	private EurekaClient eurekaClient;
	private TurbineProperties turbineProperties;
	private InstanceInfo.Builder builder;

	@Before
	public void setUp() throws Exception {
		eurekaClient = mock(EurekaClient.class);
		turbineProperties = new TurbineProperties();
		builder = InstanceInfo.Builder.newBuilder();
	}

	@Test
	public void testSecureCombineHostPort() {
		turbineProperties.setCombineHostPort(true);
		EurekaInstanceDiscovery discovery = new EurekaInstanceDiscovery(
				turbineProperties, eurekaClient);
		String appName = "testAppName";
		int port = 8080;
		int securePort = 8443;
		String hostName = "myhost";
		InstanceInfo instanceInfo = builder.setAppName(appName)
				.setHostName(hostName)
				.setPort(port)
				.setSecurePort(securePort)
				.enablePort(InstanceInfo.PortType.SECURE, true)
				.build();
		Instance instance = discovery.marshall(instanceInfo);
		assertEquals("port is wrong", String.valueOf(port), instance.getAttributes().get("port"));
		assertEquals("securePort is wrong", String.valueOf(securePort), instance.getAttributes().get("securePort"));

		String urlPath = SpringClusterMonitor.ClusterConfigBasedUrlClosure.getUrlPath(instance);
		assertEquals("url is wrong", "https://"+hostName+":"+securePort+"/hystrix.stream", urlPath);
	}

	@Test
	public void testCombineHostPort() {
		turbineProperties.setCombineHostPort(true);
		EurekaInstanceDiscovery discovery = new EurekaInstanceDiscovery(
			turbineProperties, eurekaClient);
		String appName = "testAppName";
		int port = 8080;
		String hostName = "myhost";
		InstanceInfo instanceInfo = builder.setAppName(appName)
				.setHostName(hostName)
				.setPort(port)
				.build();
		Instance instance = discovery.marshall(instanceInfo);
		assertEquals("hostname is wrong", hostName+":"+port, instance.getHostname());
		assertEquals("port is wrong", String.valueOf(port), instance.getAttributes().get("port"));

		String urlPath = SpringClusterMonitor.ClusterConfigBasedUrlClosure.getUrlPath(instance);
		assertEquals("url is wrong", "http://"+hostName+":"+port+"/hystrix.stream", urlPath);

		String clusterName = discovery.getClusterName(instanceInfo);
		assertEquals("clusterName is wrong", appName.toUpperCase(), clusterName);
	}

	@Test
	public void testGetClusterName() {
		EurekaInstanceDiscovery discovery = new EurekaInstanceDiscovery(
				turbineProperties, eurekaClient);
		String appName = "testAppName";
		InstanceInfo instanceInfo = builder.setAppName(appName)
				.build();
		String clusterName = discovery.getClusterName(instanceInfo);
		assertEquals("clusterName is wrong", appName.toUpperCase(), clusterName);
	}

	@Test
	public void testGetPort() {
		EurekaInstanceDiscovery discovery = new EurekaInstanceDiscovery(
				turbineProperties, eurekaClient);
		String appName = "testAppName";
		int port = 8080;
		String hostName = "myhost";
		InstanceInfo instanceInfo = builder.setAppName(appName)
				.setHostName(hostName)
				.setPort(port)
				.build();
		Instance instance = discovery.marshall(instanceInfo);
		assertEquals("port is wrong", String.valueOf(port), instance.getAttributes().get("port"));

		String urlPath = SpringClusterMonitor.ClusterConfigBasedUrlClosure.getUrlPath(instance);
		assertEquals("url is wrong", "http://"+hostName+":"+port+"/hystrix.stream", urlPath);
	}

	@Test
	public void testGetSecurePort() {
		EurekaInstanceDiscovery discovery = new EurekaInstanceDiscovery(
				turbineProperties, eurekaClient);
		String appName = "testAppName";
		int port = 8080;
		int securePort = 8443;
		String hostName = "myhost";
		InstanceInfo instanceInfo = builder.setAppName(appName)
				.setHostName(hostName)
				.setPort(port)
				.setSecurePort(securePort)
				.enablePort(InstanceInfo.PortType.SECURE, true)
				.build();
		Instance instance = discovery.marshall(instanceInfo);
		assertEquals("port is wrong", String.valueOf(port), instance.getAttributes().get("port"));
		assertEquals("securePort is wrong", String.valueOf(securePort), instance.getAttributes().get("securePort"));

		String urlPath = SpringClusterMonitor.ClusterConfigBasedUrlClosure.getUrlPath(instance);
		assertEquals("url is wrong", "https://"+hostName+":"+securePort+"/hystrix.stream", urlPath);
	}

	@Test
	public void testGetClusterNameCustomExpression() {
		turbineProperties.setClusterNameExpression("aSGName");
		EurekaInstanceDiscovery discovery = new EurekaInstanceDiscovery(turbineProperties, eurekaClient);
		String asgName = "myAsgName";
		InstanceInfo instanceInfo = builder
				.setAppName("testApp").setASGName(asgName).build();
		String clusterName = discovery.getClusterName(instanceInfo);
		assertEquals("clusterName is wrong", asgName, clusterName);
	}

	@Test
	public void testGetClusterNameInstanceMetadataMapExpression() {
		turbineProperties.setClusterNameExpression("metadata['cluster']");
		EurekaInstanceDiscovery discovery = new EurekaInstanceDiscovery(turbineProperties, eurekaClient);
		String metadataProperty = "myCluster";
		InstanceInfo instanceInfo = builder
				.setAppName("testApp").add("cluster", metadataProperty).build();
		String clusterName = discovery.getClusterName(instanceInfo);
		assertEquals("clusterName is wrong", metadataProperty, clusterName);
	}

}
