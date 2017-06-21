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

import java.util.UUID;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.netflix.appinfo.ApplicationInfoManager;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.cloud.netflix.eureka.server.doc.RequestVerifierFilter.verify;

@RunWith(SpringJUnit4ClassRunner.class)
public class AppRegistrationTests extends AbstractDocumentationTests {

	@Autowired
	private EurekaInstanceConfigBean instanceConfig;
	@Autowired
	private ApplicationInfoManager applicationInfoManager;

	@Test
	public void addApp() throws Exception {
		instanceConfig.setAppname("foo");
		instanceConfig.setInstanceId(UUID.randomUUID().toString());
		instanceConfig.setHostname("foo.example.com");
		applicationInfoManager.initComponent(instanceConfig);
		assure("add-app", applicationInfoManager.getInfo())
				.filter(verify("$.instance.app").json("$.instance.hostName")
						.json("$.instance[?(@.status=='STARTING')]")
						.json("$.instance.instanceId")
						.json("$.instance.dataCenterInfo.name"))
				.when().post("/eureka/apps/FOO").then().assertThat().statusCode(is(204));
		assure("starting-app").accept("application/json").when().get("/eureka/apps")
				.then().assertThat()
				.body("applications.application", hasSize(1),
						"applications.application[0].instance[0].status",
						equalTo("STARTING"))
				.statusCode(is(200));
		assure("up-app")
				.filter(verify(
						WireMock.put(WireMock.urlPathMatching("/eureka/apps/FOO/.*"))
								.withQueryParam("value", WireMock.matching("UP"))))
				.when()
				.put("/eureka/apps/FOO/{id}/status?value={value}",
						applicationInfoManager.getInfo().getInstanceId(), "UP")
				.then().assertThat().statusCode(is(200));
		assure("one-app").accept("application/json").when().get("/eureka/apps").then()
				.assertThat().body("applications.application", hasSize(1))
				.statusCode(is(200));
		assure("delete-app").when()
				.delete("/eureka/apps/FOO/{id}",
						applicationInfoManager.getInfo().getInstanceId())
				.then().assertThat().statusCode(is(200));
	}

	@Test
	public void emptyApps() {
		assure().when().accept("application/json").get("/eureka/apps").then().assertThat()
				.body("applications.application", emptyIterable()).statusCode(is(200));
	}

}
