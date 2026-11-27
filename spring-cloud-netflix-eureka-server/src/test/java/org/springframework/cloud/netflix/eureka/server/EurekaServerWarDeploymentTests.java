/*
 * Copyright 2026-present the original author or authors.
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

package org.springframework.cloud.netflix.eureka.server;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationProperties;
import org.springframework.cloud.commons.util.UtilAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.cloud.netflix.eureka.config.DiscoveryClientOptionalArgsConfiguration;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaAutoServiceRegistration;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class EurekaServerWarDeploymentTests {

	@Test
	void startsWithoutEmbeddedTomcat() {
		new WebApplicationContextRunner()
			.withClassLoader(new FilteredClassLoader("org.apache.catalina", "org.apache.coyote", "org.apache.tomcat",
					"org.springframework.boot.tomcat"))
			.withConfiguration(AutoConfigurations.of(UtilAutoConfiguration.class,
					DiscoveryClientOptionalArgsConfiguration.class, EurekaClientAutoConfiguration.class,
					EurekaServerAutoConfiguration.class, EurekaServerJerseyClientAutoConfiguration.class))
			.withUserConfiguration(EurekaServerMarkerConfiguration.class)
			.withBean(AutoServiceRegistrationProperties.class, AutoServiceRegistrationProperties::new)
			.withPropertyValues("eureka.client.register-with-eureka=false", "eureka.client.fetch-registry=false")
			.run(context -> {
				assertThat(context).hasNotFailed();
				assertThat(context.getClassLoader().loadClass(WebServerInitializedEvent.class.getName()))
					.isEqualTo(WebServerInitializedEvent.class);
				assertThat(context).hasBean("eurekaVersionFilterRegistration");
				assertThat(context.getBean(EurekaAutoServiceRegistration.class).isRunning()).isTrue();
				await().until(() -> context.getBean(EurekaServerInitializerConfiguration.class).isRunning());
			});
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "/external" })
	void rewritesApiRequestsUsingServletContextPath(String contextPath) throws Exception {
		assertFilteredPath(contextPath, "/eureka/apps", contextPath + "/eureka/v2/apps");
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "/external" })
	void leavesVersionedAndStaticRequestsUnchanged(String contextPath) throws Exception {
		for (String path : new String[] { "/eureka/v2/apps", "/eureka/css/wro.css", "/eureka/js/wro.js",
				"/eureka/fonts/test.woff", "/eureka/images/logo.png" }) {
			assertFilteredPath(contextPath, path, contextPath + path);
		}
	}

	private void assertFilteredPath(String contextPath, String path, String expectedPath) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", contextPath + path);
		request.setContextPath(contextPath);
		new EurekaServerAutoConfiguration().eurekaVersionFilterRegistration(new MockEnvironment())
			.getFilter()
			.doFilter(request, new MockHttpServletResponse(),
					(filteredRequest, response) -> assertThat(((HttpServletRequest) filteredRequest).getRequestURI())
						.isEqualTo(expectedPath));
	}

}
