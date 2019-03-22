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

package org.springframework.cloud.netflix.turbine;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.turbine.discovery.Instance;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Spencer Gibb
 */
public class EurekaInstanceDiscoveryTests {

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
		EurekaInstanceDiscovery discovery = new EurekaInstanceDiscovery(turbineProperties,
				eurekaClient);
		String appName = "testAppName";
		int port = 8080;
		int securePort = 8443;
		String hostName = "myhost";
		InstanceInfo instanceInfo = builder.setAppName(appName).setHostName(hostName)
				.setPort(port).setSecurePort(securePort)
				.enablePort(InstanceInfo.PortType.SECURE, true).build();
		Instance instance = discovery.marshall(instanceInfo);
		assertThat(instance.getAttributes().get("port")).as("port is wrong")
				.isEqualTo(String.valueOf(port));
		assertThat(instance.getAttributes().get("securePort")).as("securePort is wrong")
				.isEqualTo(String.valueOf(securePort));

		String urlPath = SpringClusterMonitor.ClusterConfigBasedUrlClosure
				.getUrlPath(instance);
		assertThat(urlPath).as("url is wrong").isEqualTo(
				"https://" + hostName + ":" + securePort + "/actuator/hystrix.stream");
	}

	@Test
	public void testCombineHostPort() {
		turbineProperties.setCombineHostPort(true);
		EurekaInstanceDiscovery discovery = new EurekaInstanceDiscovery(turbineProperties,
				eurekaClient);
		String appName = "testAppName";
		int port = 8080;
		String hostName = "myhost";
		InstanceInfo instanceInfo = builder.setAppName(appName).setHostName(hostName)
				.setPort(port).build();
		Instance instance = discovery.marshall(instanceInfo);
		assertThat(instance.getHostname()).as("hostname is wrong")
				.isEqualTo(hostName + ":" + port);
		assertThat(instance.getAttributes().get("port")).as("port is wrong")
				.isEqualTo(String.valueOf(port));

		String urlPath = SpringClusterMonitor.ClusterConfigBasedUrlClosure
				.getUrlPath(instance);
		assertThat(urlPath).as("url is wrong").isEqualTo(
				"http://" + hostName + ":" + port + "/actuator/hystrix.stream");

		String clusterName = discovery.getClusterName(instanceInfo);
		assertThat(clusterName).as("clusterName is wrong")
				.isEqualTo(appName.toUpperCase());
	}

	@Test
	public void testUseManagementPortFromMetadata() {
		EurekaInstanceDiscovery discovery = new EurekaInstanceDiscovery(turbineProperties,
				eurekaClient);
		String appName = "testAppName";
		int port = 8080;
		int managementPort = 8081;
		String hostName = "myhost";
		InstanceInfo instanceInfo = builder.setAppName(appName).setHostName(hostName)
				.setPort(port).build();
		instanceInfo.getMetadata().put("management.port", "8081");
		Instance instance = discovery.marshall(instanceInfo);
		assertThat(instance.getHostname()).as("hostname is wrong")
				.isEqualTo(hostName + ":" + managementPort);
		assertThat(instance.getAttributes().get("port")).as("port is wrong")
				.isEqualTo(String.valueOf(managementPort));

		String urlPath = SpringClusterMonitor.ClusterConfigBasedUrlClosure
				.getUrlPath(instance);
		assertThat(urlPath).as("url is wrong").isEqualTo(
				"http://" + hostName + ":" + managementPort + "/actuator/hystrix.stream");

		String clusterName = discovery.getClusterName(instanceInfo);
		assertThat(clusterName).as("clusterName is wrong")
				.isEqualTo(appName.toUpperCase());
	}

	@Test
	public void testGetClusterName() {
		EurekaInstanceDiscovery discovery = new EurekaInstanceDiscovery(turbineProperties,
				eurekaClient);
		String appName = "testAppName";
		InstanceInfo instanceInfo = builder.setAppName(appName).build();
		String clusterName = discovery.getClusterName(instanceInfo);
		assertThat(clusterName).as("clusterName is wrong")
				.isEqualTo(appName.toUpperCase());
	}

	@Test
	public void testGetPort() {
		EurekaInstanceDiscovery discovery = new EurekaInstanceDiscovery(turbineProperties,
				eurekaClient);
		String appName = "testAppName";
		int port = 8080;
		String hostName = "myhost";
		InstanceInfo instanceInfo = builder.setAppName(appName).setHostName(hostName)
				.setPort(port).build();
		Instance instance = discovery.marshall(instanceInfo);
		assertThat(instance.getAttributes().get("port")).as("port is wrong")
				.isEqualTo(String.valueOf(port));

		String urlPath = SpringClusterMonitor.ClusterConfigBasedUrlClosure
				.getUrlPath(instance);
		assertThat(urlPath).as("url is wrong").isEqualTo(
				"http://" + hostName + ":" + port + "/actuator/hystrix.stream");
	}

	@Test
	public void testGetSecurePort() {
		EurekaInstanceDiscovery discovery = new EurekaInstanceDiscovery(turbineProperties,
				eurekaClient);
		String appName = "testAppName";
		int port = 8080;
		int securePort = 8443;
		String hostName = "myhost";
		InstanceInfo instanceInfo = builder.setAppName(appName).setHostName(hostName)
				.setPort(port).setSecurePort(securePort)
				.enablePort(InstanceInfo.PortType.SECURE, true).build();
		Instance instance = discovery.marshall(instanceInfo);
		assertThat(instance.getAttributes().get("port")).as("port is wrong")
				.isEqualTo(String.valueOf(port));
		assertThat(instance.getAttributes().get("securePort")).as("securePort is wrong")
				.isEqualTo(String.valueOf(securePort));

		String urlPath = SpringClusterMonitor.ClusterConfigBasedUrlClosure
				.getUrlPath(instance);
		assertThat(urlPath).as("url is wrong").isEqualTo(
				"https://" + hostName + ":" + securePort + "/actuator/hystrix.stream");
	}

	@Test
	public void testGetClusterNameCustomExpression() {
		turbineProperties.setClusterNameExpression("aSGName");
		EurekaInstanceDiscovery discovery = new EurekaInstanceDiscovery(turbineProperties,
				eurekaClient);
		String asgName = "myAsgName";
		InstanceInfo instanceInfo = builder.setAppName("testApp").setASGName(asgName)
				.build();
		String clusterName = discovery.getClusterName(instanceInfo);
		assertThat(clusterName).as("clusterName is wrong").isEqualTo(asgName);
	}

	@Test
	public void testGetClusterNameInstanceMetadataMapExpression() {
		turbineProperties.setClusterNameExpression("metadata['cluster']");
		EurekaInstanceDiscovery discovery = new EurekaInstanceDiscovery(turbineProperties,
				eurekaClient);
		String metadataProperty = "myCluster";
		InstanceInfo instanceInfo = builder.setAppName("testApp")
				.add("cluster", metadataProperty).build();
		String clusterName = discovery.getClusterName(instanceInfo);
		assertThat(clusterName).as("clusterName is wrong").isEqualTo(metadataProperty);
	}

}
