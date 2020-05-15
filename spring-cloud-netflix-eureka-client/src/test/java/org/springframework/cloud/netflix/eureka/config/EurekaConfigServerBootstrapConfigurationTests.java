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

package org.springframework.cloud.netflix.eureka.config;

import java.util.Collections;
import java.util.List;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Applications;
import com.netflix.discovery.shared.transport.EurekaHttpClient;
import com.netflix.discovery.shared.transport.EurekaHttpResponse;
import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.OutputCaptureRule;
import org.springframework.cloud.config.client.ConfigServerInstanceProvider;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.http.RestTemplateEurekaHttpClient;
import org.springframework.cloud.test.ClassPathExclusions;
import org.springframework.cloud.test.ModifiedClassPathRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Spencer Gibb
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions("spring-webflux-*")
public class EurekaConfigServerBootstrapConfigurationTests {

	@Rule
	public OutputCaptureRule output = new OutputCaptureRule();

	@Test
	public void offByDefault() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations
						.of(EurekaConfigServerBootstrapConfiguration.class))
				.run(context -> {
					assertThat(context).doesNotHaveBean(EurekaClientConfigBean.class);
					assertThat(context).doesNotHaveBean(EurekaHttpClient.class);
					assertThat(context)
							.doesNotHaveBean(ConfigServerInstanceProvider.Function.class);
				});
	}

	@Test
	public void properBeansCreatedWhenEnabled() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations
						.of(EurekaConfigServerBootstrapConfiguration.class))
				.withPropertyValues("spring.cloud.config.discovery.enabled=true")
				.run(context -> {
					assertThat(context).hasSingleBean(EurekaClientConfigBean.class);
					assertThat(context).hasSingleBean(RestTemplateEurekaHttpClient.class);
					assertThat(context)
							.hasSingleBean(ConfigServerInstanceProvider.Function.class);
				});
	}

	@Test
	public void eurekaConfigServerInstanceProviderCalled() {
		// FIXME: why do I need to do this? (fails in maven build without it.
		TomcatURLStreamHandlerFactory.disable();
		new SpringApplicationBuilder(TestConfigDiscoveryConfiguration.class).properties(
				"spring.cloud.config.discovery.enabled=true",
				"spring.main.sources="
						+ TestConfigDiscoveryBootstrapConfiguration.class.getName(),
				"logging.level.org.springframework.cloud.netflix.eureka.config=DEBUG")
				.run();
		assertThat(output).contains(
				"eurekaConfigServerInstanceProvider finding instances for configserver")
				.contains(
						"eurekaConfigServerInstanceProvider found 1 instance(s) for configserver");
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	protected static class TestConfigDiscoveryConfiguration {

		@Bean
		public EurekaClient getClient() {
			return mock(CloudEurekaClient.class);
		}

	}

	@Configuration
	protected static class TestConfigDiscoveryBootstrapConfiguration {

		@SuppressWarnings("unchecked")
		@Bean
		public EurekaHttpClient mockEurekaHttpClient() {
			InstanceInfo instanceInfo = InstanceInfo.Builder.newBuilder()
					.setAppName("configserver").build();
			List<InstanceInfo> instanceInfos = Collections.singletonList(instanceInfo);

			Applications applications = mock(Applications.class);
			when(applications.getInstancesByVirtualHostName("configserver"))
					.thenReturn(instanceInfos);

			EurekaHttpResponse<Applications> response = mock(EurekaHttpResponse.class);
			when(response.getStatusCode()).thenReturn(200);
			when(response.getEntity()).thenReturn(applications);

			EurekaHttpClient client = mock(EurekaHttpClient.class);
			when(client.getApplications("us-east-1")).thenReturn(response);
			return client;
		}

	}

}
