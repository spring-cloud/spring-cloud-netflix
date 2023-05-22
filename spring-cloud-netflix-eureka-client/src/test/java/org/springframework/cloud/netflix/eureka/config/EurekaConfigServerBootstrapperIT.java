/*
 * Copyright 2013-2023 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.converters.jackson.serializer.InstanceInfoJsonBeanSerializer;
import com.netflix.discovery.shared.Application;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.netflix.eureka.http.EurekaApplications;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * @author Ryan Baxter
 */
@Testcontainers
public class EurekaConfigServerBootstrapperIT {

	public static final DockerImageName MOCKSERVER_IMAGE = DockerImageName.parse("mockserver/mockserver")
			.withTag("mockserver-" + MockServerClient.class.getPackage().getImplementationVersion());

	@Container
	static MockServerContainer mockServer = new MockServerContainer(MOCKSERVER_IMAGE);

	private ConfigurableApplicationContext context;

	@BeforeEach
	void before() {

	}

	@AfterEach
	void after() {
		this.context.close();
	}

	@Test
	public void contextLoads() throws JsonProcessingException {
		Environment environment = new Environment("test", "default");
		Map<String, Object> properties = new HashMap<>();
		properties.put("hello", "world");
		PropertySource p = new PropertySource("p1", properties);
		environment.add(p);
		ObjectMapper objectMapper = new ObjectMapper();
		SimpleModule jsonModule = new SimpleModule();
		jsonModule.setSerializerModifier(createJsonSerializerModifier());
		objectMapper.registerModule(jsonModule);
		try (MockServerClient mockServerClient = new MockServerClient(mockServer.getHost(),
				mockServer.getMappedPort(MockServerContainer.PORT))) {
			mockServerClient.when(request().withPath("/application/default"))
					.respond(response().withBody(objectMapper.writeValueAsString(environment))
							.withHeader("content-type", "application/json"));
			InstanceInfo configServerInstanceInfo = InstanceInfo.Builder.newBuilder()
					.setVIPAddress("eureka-configserver").setInstanceId("eureka-configserver")
					.setAppName("eureka-configserver").setStatus(InstanceInfo.InstanceStatus.UP)
					.enablePort(InstanceInfo.PortType.UNSECURE, true).setHostName("localhost")
					.setPort(mockServer.getMappedPort(MockServerContainer.PORT)).build();
			Application configServer = new Application("eureka-configserver",
					Collections.singletonList(configServerInstanceInfo));
			EurekaApplications eurekaApplications = new EurekaApplications("hashcode", 0L,
					Collections.singletonList(configServer));
			mockServerClient.when(request().withPath("/apps/"))
					.respond(response()
							.withBody("{\"applications\":" + objectMapper.writeValueAsString(eurekaApplications) + "}")
							.withHeader("content-type", "application/json"));
			this.context = setup().run();
			assertThat(this.context.getEnvironment().getProperty("hello")).isEqualTo("world");
		}

	}

	public static BeanSerializerModifier createJsonSerializerModifier() {
		return new BeanSerializerModifier() {
			@Override
			public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc,
					JsonSerializer<?> serializer) {
				if (beanDesc.getBeanClass().isAssignableFrom(InstanceInfo.class)) {
					return new InstanceInfoJsonBeanSerializer((BeanSerializerBase) serializer, false);
				}
				return serializer;
			}
		};
	}

	SpringApplicationBuilder setup(String... env) {
		SpringApplicationBuilder builder = new SpringApplicationBuilder(TestConfig.class)
				.properties(addDefaultEnv(env));
		return builder;
	}

	private String[] addDefaultEnv(String[] env) {
		Set<String> set = new LinkedHashSet<>();
		if (env != null && env.length > 0) {
			set.addAll(Arrays.asList(env));
		}
		set.add("spring.config.import=classpath:bootstrapper.yaml");
		set.add("spring.cloud.config.enabled=true");
		set.add("spring.cloud.service-registry.auto-registration.enabled=false");
		set.add("eureka.client.serviceUrl.defaultZone=http://" + mockServer.getHost() + ":"
				+ mockServer.getMappedPort(MockServerContainer.PORT));
		return set.toArray(new String[0]);
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	static class TestConfig {

	}

}
