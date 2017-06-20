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

package org.springframework.cloud.netflix.eureka.server.doc;

import com.netflix.appinfo.ApplicationInfoManager;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;

@RunWith(SpringJUnit4ClassRunner.class)
public class RegisteredAppsTests extends AbstractDocumentationTests {

	@Autowired
	private EurekaInstanceConfigBean instanceConfig;
	@Autowired
	private EurekaClientConfigBean clientConfig;
	@Autowired
	private ApplicationInfoManager applicationInfoManager;
	@Autowired
	private ApplicationEventPublisher publisher;

	private static CloudEurekaClient client;

	@Before
	public void setUp() throws Exception {
		if (client == null) {
			instanceConfig.setAppname("foo");
			instanceConfig.setLeaseRenewalIntervalInSeconds(1);
			applicationInfoManager.initComponent(instanceConfig);
			clientConfig.setInitialInstanceInfoReplicationIntervalSeconds(0);
			clientConfig.setRegisterWithEureka(true);
			client = new CloudEurekaClient(applicationInfoManager, clientConfig,
					publisher);
			// Give registration a chance to work
			while (client.getLastSuccessfulHeartbeatTimePeriod() < 0) {
				Thread.sleep(100L);
			}
		}
	}

	@AfterClass
	public static void cleanUp() {
		if (client != null) {
			client.shutdown();
		}
	}

	@Test
	public void registeredApps() throws Exception {
		assure().accept("application/json").when().get("/eureka/apps").then().assertThat()
				.body("applications.application", hasSize(1)).statusCode(is(200));
	}

}
