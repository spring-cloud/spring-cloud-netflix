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

import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.cloud.netflix.eureka.server.doc.RequestVerifierFilter.verify;

@RunWith(SpringJUnit4ClassRunner.class)
public class AppRegistrationTests extends AbstractDocumentationTests {

	@Test
	public void startingApp() throws Exception {
		register("foo");
		document().accept("application/json").when().get("/eureka/apps").then()
				.assertThat()
				.body("applications.application", hasSize(1),
						"applications.application[0].instance[0].status",
						equalTo("STARTING"))
				.statusCode(is(200));
	}

	@Test
	public void addInstance() throws Exception {
		document(instance("foo"))
				.filter(verify("$.instance.app").json("$.instance.hostName")
						.json("$.instance[?(@.status=='STARTING')]")
						.json("$.instance.instanceId")
						.json("$.instance.dataCenterInfo.name"))
				.when().post("/eureka/apps/FOO").then().assertThat().statusCode(is(204));
	}

	@Test
	public void setStatus() throws Exception {
		String id = register("foo").getInstanceId();
		document()
				.filter(verify(put(urlPathMatching("/eureka/apps/FOO/.*/status"))
						.withQueryParam("value", matching("UP"))))
				.when().put("/eureka/apps/FOO/{id}/status?value={value}", id, "UP").then()
				.assertThat().statusCode(is(200));
	}

	@Test
	public void allApps() throws Exception {
		register("foo");
		document().accept("application/json").when().get("/eureka/apps").then()
				.assertThat().body("applications.application", hasSize(1))
				.statusCode(is(200));
	}

	@Test
	public void delta() throws Exception {
		register("foo");
		document().accept("application/json").when().get("/eureka/apps/delta").then()
				.assertThat().body("applications.application", hasSize(1))
				.statusCode(is(200));
	}

	@Test
	public void oneInstance() throws Exception {
		String id = UUID.randomUUID().toString();
		register("foo", id);
		document().filter(verify(get(urlPathMatching("/eureka/apps/FOO/.*"))))
				.accept("application/json").when().get("/eureka/apps/FOO/{id}", id).then()
				.assertThat().body("instance.app", equalTo("FOO")).statusCode(is(200));
	}

	@Test
	public void lookupInstance() throws Exception {
		String id = register("foo").getInstanceId();
		document().filter(verify(get(urlPathMatching("/eureka/instances/.*"))))
				.accept("application/json").when().get("/eureka/instances/{id}", id)
				.then().assertThat().body("instance.app", equalTo("FOO"))
				.statusCode(is(200));
	}

	@Test
	public void renew() throws Exception {
		String id = register("foo").getInstanceId();
		document().filter(verify(put(urlPathMatching("/eureka/apps/FOO/.*"))))
				.accept("application/json").when().put("/eureka/apps/FOO/{id}", id).then()
				.assertThat().statusCode(is(200));
	}

	@Test
	public void updateMetadata() throws Exception {
		String id = register("foo").getInstanceId();
		document()
				.filter(verify(put(urlPathMatching("/eureka/apps/FOO/.*/metadata"))
						.withQueryParam("key", matching(".*"))))
				.accept("application/json").when()
				.put("/eureka/apps/FOO/{id}/metadata?key=value", id).then().assertThat()
				.statusCode(is(200));
		assertThat(instance().getMetadata()).containsEntry("key", "value");
	}

	@Test
	public void deleteInstance() throws Exception {
		String id = register("foo").getInstanceId();
		document().filter(verify(delete(urlPathMatching("/eureka/apps/FOO/.*")))).when()
				.delete("/eureka/apps/FOO/{id}", id).then().assertThat()
				.statusCode(is(200));
	}

	@Test
	public void emptyApps() {
		document().when().accept("application/json").get("/eureka/apps").then()
				.assertThat().body("applications.application", emptyIterable())
				.statusCode(is(200));
	}

}
