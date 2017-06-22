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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;

@RunWith(SpringJUnit4ClassRunner.class)
// TODO: maybe this should be the default (the test fails without it because the JSON is
// invalid)
@TestPropertySource(properties = "eureka.server.minAvailableInstancesForPeerReplication=0")
public class EurekaServerTests extends AbstractDocumentationTests {

	@Test
	public void serverStatus() throws Exception {
		register("foo", UUID.randomUUID().toString());
		document().accept("application/json").when().get("/eureka/status").then()
				.assertThat().body("generalStats", notNullValue(), "applicationStats",
						notNullValue(), "instanceInfo", notNullValue())
				.statusCode(is(200));
	}

}
