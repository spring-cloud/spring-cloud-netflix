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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.emptyIterable;

@RunWith(SpringJUnit4ClassRunner.class)
public class EmptyAppsTests extends AbstractDocumentationTests {

	@Autowired
	private EurekaInstanceConfigBean instanceConfig;
	@Autowired
	private ApplicationInfoManager applicationInfoManager;

	@Test
	public void addApp() throws Exception {
		instanceConfig.setAppname("foo");
		instanceConfig.setInstanceId("unique-id");
		instanceConfig.setHostname("foo.example.com");
		applicationInfoManager.initComponent(instanceConfig);
		assure("add-app", applicationInfoManager.getInfo()).when()
				.post("/eureka/apps/FOO").then().assertThat().statusCode(is(204));
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
