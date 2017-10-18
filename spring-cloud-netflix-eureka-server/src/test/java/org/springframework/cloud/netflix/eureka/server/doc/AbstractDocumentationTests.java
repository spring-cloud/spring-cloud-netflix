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

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.filter.Filter;
import com.jayway.restassured.specification.RequestSpecification;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.eureka.registry.PeerAwareInstanceRegistryImpl;

import org.junit.After;
import org.junit.Rule;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.contract.wiremock.restdocs.WireMockSnippet;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.cloud.netflix.eureka.server.doc.AbstractDocumentationTests.Application;
import org.springframework.context.annotation.Configuration;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.restassured.RestAssuredRestDocumentation;
import org.springframework.restdocs.restassured.RestDocumentationFilter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.restassured.RestAssuredRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.restassured.operation.preprocess.RestAssuredPreprocessors.modifyUris;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = WebEnvironment.RANDOM_PORT, value = {
		"spring.jmx.enabled=false", "management.security.enabled=false" })
@DirtiesContext
public abstract class AbstractDocumentationTests {

	@LocalServerPort
	private int port = 0;

	@Autowired
	private PeerAwareInstanceRegistryImpl registry;

	@Autowired
	private EurekaInstanceConfigBean instanceConfig;

	@Autowired
	private ApplicationInfoManager applicationInfoManager;

	@Rule
	public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation(
			"target/generated-snippets");

	@After
	public void init() {
		registry.clearRegistry();
		ReflectionTestUtils.setField(registry, "responseCache", null);
		registry.initializedResponseCache();
	}

	protected InstanceInfo register(String name) {
		return register(name, UUID.randomUUID().toString());
	}

	protected InstanceInfo register(String name, String id) {
		registry.register(instance(name, id), false);
		return instance();
	}

	protected InstanceInfo instance(String name) {
		return instance(name, UUID.randomUUID().toString());
	}

	protected InstanceInfo instance(String name, String id) {
		instanceConfig.setAppname(name);
		instanceConfig.setInstanceId(id);
		instanceConfig.setHostname("foo.example.com");
		applicationInfoManager.initComponent(instanceConfig);
		return applicationInfoManager.getInfo();
	}

	protected InstanceInfo instance() {
		return applicationInfoManager.getInfo();
	}

	private RestDocumentationFilter filter(String name) {
		return RestAssuredRestDocumentation.document(name,
				preprocessRequest(modifyUris().host("eureka.example.com").removePort(),
						prettyPrint()),
				preprocessResponse(prettyPrint()));
	}

	private RequestSpecification spec(Filter... filters) {
		return spec(null, filters);
	}

	private RequestSpecification spec(Object body, Filter... filters) {
		RequestSpecBuilder builder = new RequestSpecBuilder()
				.addFilter(documentationConfiguration(this.restDocumentation).snippets()
						.withAdditionalDefaults(new WireMockSnippet()));
		for (Filter filter : filters) {
			builder = builder.addFilter(filter);
		}
		RequestSpecification spec = builder.setPort(this.port).build();
		if (body != null) {
			spec.contentType("application/json").body(body, new EurekaObjectMapper());
		}
		return spec;
	}

	protected RequestSpecification document() {
		return document("{method-name}");
	}

	protected RequestSpecification document(Object body) {
		RestDocumentationFilter filter = filter("{method-name}");
		RequestSpecification assured = RestAssured.given(spec(body, filter));
		return assured.filter(filter);
	}

	protected RequestSpecification document(String name, Object body) {
		RestDocumentationFilter filter = filter(name);
		RequestSpecification assured = RestAssured.given(spec(body, filter));
		return assured.filter(filter);
	}

	protected RequestSpecification document(String name) {
		RestDocumentationFilter filter = filter(name);
		return RestAssured.given(spec(filter)).filter(filter);
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableEurekaServer
	protected static class Application {
		public static void main(String[] args) {
			new SpringApplicationBuilder(Application.class).properties(
					"spring.application.name=eureka", "management.security.enabled=false",
					"eureka.client.registerWithEureka=true").run(args);
		}
	}

}
