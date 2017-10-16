/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.http;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider;
import com.netflix.discovery.shared.resolver.DefaultEndpoint;
import com.netflix.discovery.shared.transport.EurekaHttpClient;

/**
 * @author Daniel Lavoie
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = EurekaServerMockApplication.class, properties = { "debug=true",
		"security.basic.enabled=true" }, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class RestTemplateEurekaHttpClientTest {
	@Autowired
	private InetUtils inetUtils;

	@Value("http://${security.user.name}:${security.user.password}@localhost:${local.server.port}")
	private String serviceUrl;

	private EurekaHttpClient eurekaHttpClient;
	private InstanceInfo info;

	@Before
	public void setup() {
		eurekaHttpClient = new RestTemplateTransportClientFactory()
				.newClient(new DefaultEndpoint(serviceUrl));

		EurekaInstanceConfigBean config = new EurekaInstanceConfigBean(inetUtils);

		String appname = "customapp";
		config.setIpAddress("127.0.0.1");
		config.setHostname("localhost");
		config.setAppname(appname);
		config.setVirtualHostName(appname);
		config.setSecureVirtualHostName(appname);
		config.setNonSecurePort(4444);
		config.setSecurePort(8443);
		config.setInstanceId("127.0.0.1:customapp:4444");

		info = new EurekaConfigBasedInstanceInfoProvider(config).get();
	}

	@Test
	public void testRegister() {
		Assert.assertEquals(HttpStatus.OK.value(),
				eurekaHttpClient.register(info).getStatusCode());
	}

	@Test
	public void testCancel() {
		Assert.assertEquals(HttpStatus.OK.value(),
				eurekaHttpClient.cancel("test", "test").getStatusCode());
	}

	@Test
	public void testSendHeartBeat() {
		Assert.assertEquals(HttpStatus.OK.value(), eurekaHttpClient
				.sendHeartBeat("test", "test", info, null).getStatusCode());
	}

	@Test
	public void testStatusUpdate() {
		Assert.assertEquals(HttpStatus.OK.value(), eurekaHttpClient
				.statusUpdate("test", "test", InstanceStatus.UP, info).getStatusCode());
	}

	@Test
	public void testDeleteStatusOverride() {
		Assert.assertEquals(HttpStatus.OK.value(), eurekaHttpClient
				.deleteStatusOverride("test", "test", info).getStatusCode());
	}

	@Test
	public void testGetApplications() {
		Assert.assertNotNull(eurekaHttpClient.getApplications().getEntity());
		Assert.assertNotNull(eurekaHttpClient.getApplications("us", "eu").getEntity());
	}

	@Test
	public void testGetDelta() {
		eurekaHttpClient.getDelta().getEntity();
		eurekaHttpClient.getDelta("us", "eu").getEntity();
	}

	@Test
	public void testGetVips() {
		eurekaHttpClient.getVip("test");
		eurekaHttpClient.getVip("test", "us", "eu");
	}

	@Test
	public void testGetSecureVip() {
		eurekaHttpClient.getSecureVip("test");
		eurekaHttpClient.getSecureVip("test", "us", "eu");
	}

	@Test
	public void testGetApplication() {
		eurekaHttpClient.getApplication("test");
	}

	@Test
	public void testGetInstance() {
		eurekaHttpClient.getInstance("test");
		eurekaHttpClient.getInstance("test", "test");
	}
}
