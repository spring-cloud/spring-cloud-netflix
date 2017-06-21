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

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.filter.Filter;
import com.jayway.restassured.specification.RequestSpecification;

import org.junit.Rule;
import org.junit.runner.RunWith;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.contract.wiremock.restdocs.WireMockSnippet;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.cloud.netflix.eureka.server.doc.AbstractDocumentationTests.Application;
import org.springframework.context.annotation.Configuration;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.restassured.RestAssuredRestDocumentation;
import org.springframework.restdocs.restassured.RestDocumentationFilter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.restassured.RestAssuredRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.restassured.operation.preprocess.RestAssuredPreprocessors.modifyUris;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = WebEnvironment.DEFINED_PORT, value = {
		"spring.jmx.enabled=true", "management.security.enabled=false" })
@DirtiesContext
public abstract class AbstractDocumentationTests {

	@LocalServerPort
	private int port = 0;

	@Rule
	public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation(
			"target/generated-snippets");

	private RestDocumentationFilter filter(String name) {
		return RestAssuredRestDocumentation.document(name,
				preprocessRequest(modifyUris().host("eureka.example.com").removePort(),
						prettyPrint()),
				preprocessResponse(prettyPrint()));
	}

	private RequestSpecification document(Filter... filters) {
		return document(null, filters);
	}

	private RequestSpecification document(Object body, Filter... filters) {
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

	protected RequestSpecification assure(String name, Object body) {
		RestDocumentationFilter filter = filter(name);
		RequestSpecification assured = RestAssured.given(document(body, filter));
		return assured.filter(filter);
	}

	protected RequestSpecification assure(String name) {
		RestDocumentationFilter filter = filter(name);
		return RestAssured.given(document(filter)).filter(filter);
	}

	protected RequestSpecification assure() {
		return assure("{method-name}");
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
