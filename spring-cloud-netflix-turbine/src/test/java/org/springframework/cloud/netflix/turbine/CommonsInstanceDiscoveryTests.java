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

package org.springframework.cloud.netflix.turbine;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import com.netflix.turbine.discovery.Instance;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Spencer Gibb
 */
public class CommonsInstanceDiscoveryTests {

	private DiscoveryClient discoveryClient;
	private TurbineProperties turbineProperties;

	@Before
	public void setUp() throws Exception {
		this.discoveryClient = mock(DiscoveryClient.class);
		this.turbineProperties = new TurbineProperties();
	}

	@Test
	public void testSecureCombineHostPort() {
		turbineProperties.setCombineHostPort(true);
		CommonsInstanceDiscovery discovery = createDiscovery();
		String appName = "testAppName";
		int port = 8443;
		String hostName = "myhost";
		DefaultServiceInstance serviceInstance = new DefaultServiceInstance(appName, hostName, port, true);
		Instance instance = discovery.marshall(serviceInstance);
		assertEquals("port is wrong", String.valueOf(port), instance.getAttributes().get("port"));
		assertEquals("securePort is wrong", String.valueOf(port), instance.getAttributes().get("securePort"));

		String urlPath = SpringClusterMonitor.ClusterConfigBasedUrlClosure.getUrlPath(instance);
		assertEquals("url is wrong", "https://"+hostName+":"+port+"/actuator/hystrix.stream", urlPath);
	}

	@Test
	public void testCombineHostPort() {
		turbineProperties.setCombineHostPort(true);
		CommonsInstanceDiscovery discovery = createDiscovery();
		String appName = "testAppName";
		int port = 8080;
		String hostName = "myhost";
		DefaultServiceInstance serviceInstance = new DefaultServiceInstance(appName, hostName, port, false);
		Instance instance = discovery.marshall(serviceInstance);
		assertEquals("hostname is wrong", hostName+":"+port, instance.getHostname());
		assertEquals("port is wrong", String.valueOf(port), instance.getAttributes().get("port"));

		String urlPath = SpringClusterMonitor.ClusterConfigBasedUrlClosure.getUrlPath(instance);
		assertEquals("url is wrong", "http://"+hostName+":"+port+"/actuator/hystrix.stream", urlPath);

		String clusterName = discovery.getClusterName(serviceInstance);
		assertEquals("clusterName is wrong", appName, clusterName);
	}

	@Test
	public void testGetClusterName() {
		CommonsInstanceDiscovery discovery = createDiscovery();
		String appName = "testAppName";
		DefaultServiceInstance serviceInstance = new DefaultServiceInstance(appName, "myhost", 8080, false);
		String clusterName = discovery.getClusterName(serviceInstance);
		assertEquals("clusterName is wrong", appName, clusterName);
	}

	@Test
	public void testGetPort() {
		CommonsInstanceDiscovery discovery = createDiscovery();
		String appName = "testAppName";
		int port = 8080;
		String hostName = "myhost";
		DefaultServiceInstance serviceInstance = new DefaultServiceInstance(appName, hostName, port, false);
		Instance instance = discovery.marshall(serviceInstance);
		assertEquals("port is wrong", String.valueOf(port), instance.getAttributes().get("port"));

		String urlPath = SpringClusterMonitor.ClusterConfigBasedUrlClosure.getUrlPath(instance);
		assertEquals("url is wrong", "http://"+hostName+":"+port+"/actuator/hystrix.stream", urlPath);
	}

	@Test
	public void testUseManagementPortFromMetadata() {
		CommonsInstanceDiscovery discovery = createDiscovery();
		String appName = "testAppName";
		int port = 8080;
		int managementPort = 8081;
		String hostName = "myhost";
		DefaultServiceInstance serviceInstance = new DefaultServiceInstance(appName, hostName, port, false);
		serviceInstance.getMetadata().put("management.port", String.valueOf(managementPort));
		Instance instance = discovery.marshall(serviceInstance);
		assertEquals("port is wrong", String.valueOf(managementPort), instance.getAttributes().get("port"));

		String urlPath = SpringClusterMonitor.ClusterConfigBasedUrlClosure.getUrlPath(instance);
		assertEquals("url is wrong", "http://"+hostName+":"+managementPort+"/actuator/hystrix.stream", urlPath);
	}

	@Test
	public void testGetSecurePort() {
		CommonsInstanceDiscovery discovery = createDiscovery();
		String appName = "testAppName";
		//int port = 8080;
		int port = 8443;
		String hostName = "myhost";
		DefaultServiceInstance serviceInstance = new DefaultServiceInstance(appName, hostName, port, true);
		Instance instance = discovery.marshall(serviceInstance);
		assertEquals("port is wrong", String.valueOf(port), instance.getAttributes().get("port"));
		assertEquals("securePort is wrong", String.valueOf(port), instance.getAttributes().get("securePort"));

		String urlPath = SpringClusterMonitor.ClusterConfigBasedUrlClosure.getUrlPath(instance);
		assertEquals("url is wrong", "https://"+hostName+":"+port+"/actuator/hystrix.stream", urlPath);
	}

	@Test
	public void testGetClusterNameCustomExpression() {
		turbineProperties.setClusterNameExpression("host");
		CommonsInstanceDiscovery discovery = createDiscovery();
		String appName = "testAppName";
		String hostName = "myhost";
		DefaultServiceInstance serviceInstance = new DefaultServiceInstance(appName, hostName, 8080, true);
		String clusterName = discovery.getClusterName(serviceInstance);
		assertEquals("clusterName is wrong", hostName, clusterName);
	}

	@Test
	public void testGetClusterNameInstanceMetadataMapExpression() {
		turbineProperties.setClusterNameExpression("metadata['cluster']");
		CommonsInstanceDiscovery discovery = createDiscovery();
		String metadataProperty = "myCluster";
		String appName = "testAppName";
		String hostName = "myhost";
		DefaultServiceInstance serviceInstance = new DefaultServiceInstance(appName, hostName, 8080, true, Collections.singletonMap("cluster", metadataProperty));
		String clusterName = discovery.getClusterName(serviceInstance);
		assertEquals("clusterName is wrong", metadataProperty, clusterName);
	}

	private CommonsInstanceDiscovery createDiscovery() {
		return new CommonsInstanceDiscovery(turbineProperties, discoveryClient);
	}

}
