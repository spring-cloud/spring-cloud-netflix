/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.server.doc;

import java.util.UUID;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.eureka.DefaultEurekaServerContext;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.cluster.PeerEurekaNodes;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import com.netflix.eureka.registry.PeerAwareInstanceRegistryImpl;
import com.netflix.eureka.resources.ServerCodecs;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.Filter;
import io.restassured.specification.RequestSpecification;
import org.junit.After;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.cloud.netflix.eureka.server.doc.AbstractDocumentationTests.Application;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.restassured3.RestAssuredRestDocumentation;
import org.springframework.restdocs.restassured3.RestDocumentationFilter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyUris;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.documentationConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = WebEnvironment.RANDOM_PORT,
		value = { "spring.jmx.enabled=false", "management.security.enabled=false" })
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
				.addFilter(documentationConfiguration(this.restDocumentation));
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

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@EnableEurekaServer
	protected static class Application {

		private static final Logger logger = LoggerFactory
				.getLogger(DefaultEurekaServerContext.class);

		@Bean
		public EurekaServerContext testEurekaServerContext(ServerCodecs serverCodecs,
				PeerAwareInstanceRegistry registry, PeerEurekaNodes peerEurekaNodes,
				ApplicationInfoManager applicationInfoManager,
				EurekaServerConfig eurekaServerConfig) {
			return new DefaultEurekaServerContext(eurekaServerConfig, serverCodecs,
					registry, peerEurekaNodes, applicationInfoManager) {
				@Override
				public void shutdown() {
					logger.info(
							"Shutting down (except ServoControl and EurekaMonitors)..");
					registry.shutdown();
					peerEurekaNodes.shutdown();
					// ServoControl.shutdown();
					// EurekaMonitors.shutdown();
					logger.info("Shut down");
				}
			};
		}

	}

}
