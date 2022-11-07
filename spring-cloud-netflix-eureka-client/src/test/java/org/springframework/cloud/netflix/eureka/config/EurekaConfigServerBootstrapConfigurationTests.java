/*
 * Copyright 2013-2022 the original author or authors.
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
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
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
				.withConfiguration(AutoConfigurations.of(EurekaConfigServerBootstrapConfiguration.class))
				.run(this::assertEurekaBeansNotPresent);
	}

	@Test
	public void properBeansCreatedWhenDiscoveryEnabled() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(EurekaConfigServerBootstrapConfiguration.class))
				.withPropertyValues("spring.cloud.config.discovery.enabled=true").run(this::assertEurekaBeansPresent);
	}

	@Test
	public void beansNotCreatedWhenDiscoveryNotEnabled() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(EurekaConfigServerBootstrapConfiguration.class))
				.withPropertyValues("spring.cloud.config.discovery.enabled=false")
				.run(this::assertEurekaBeansNotPresent);
	}

	@Test
	public void beansNotCreatedWhenDiscoveryDisabled() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(EurekaConfigServerBootstrapConfiguration.class))
				.withPropertyValues("spring.cloud.config.discovery.disabled").run(this::assertEurekaBeansNotPresent);
	}

	@Test
	public void beansNotCreatedWhenEurekaClientEnabled() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(EurekaConfigServerBootstrapConfiguration.class))
				.withPropertyValues("eureka.client.enabled=true").run(this::assertEurekaBeansNotPresent);
	}

	@Test
	public void beansNotCreatedWhenEurekaClientNotEnabled() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(EurekaConfigServerBootstrapConfiguration.class))
				.withPropertyValues("eureka.client.enabled=false").run(this::assertEurekaBeansNotPresent);
	}

	@Test
	public void beansNotCreatedWhenEurekaClientDisabled() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(EurekaConfigServerBootstrapConfiguration.class))
				.withPropertyValues("eureka.client.disabled").run(this::assertEurekaBeansNotPresent);
	}

	@Test
	public void properBeansCreatedWhenDiscoveryEnabled_EurekaEnabled() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(EurekaConfigServerBootstrapConfiguration.class))
				.withPropertyValues("spring.cloud.config.discovery.enabled=true", "eureka.client.enabled=true")
				.run(this::assertEurekaBeansPresent);
	}

	@Test
	public void beansNotCreatedWhenDiscoveryEnabled_EurekaNotEnabled() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(EurekaConfigServerBootstrapConfiguration.class))
				.withPropertyValues("spring.cloud.config.discovery.enabled=true", "eureka.client.enabled=false")
				.run(this::assertEurekaBeansNotPresent);
	}

	@Test
	public void beansNotCreatedWhenDiscoveryNotEnabled_EurekaEnabled() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(EurekaConfigServerBootstrapConfiguration.class))
				.withPropertyValues("spring.cloud.config.discovery.enabled=false", "eureka.client.enabled=true")
				.run(this::assertEurekaBeansNotPresent);
	}

	@Test
	public void beansNotCreatedWhenDiscoveryNotEnabled_EurekaNotEnabled() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(EurekaConfigServerBootstrapConfiguration.class))
				.withPropertyValues("spring.cloud.config.discovery.enabled=false", "eureka.client.enabled=false")
				.run(this::assertEurekaBeansNotPresent);
	}

	@Test
	public void eurekaDnsConfigurationWorks() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(EurekaConfigServerBootstrapConfiguration.class))
				.withPropertyValues("spring.cloud.config.discovery.enabled=true", "eureka.client.enabled=true",
						"eureka.instance.hostname=eurekaclient1",
						"eureka.client.use-dns-for-fetching-service-urls=true",
						"eureka.client.eureka-server-d-n-s-name=myeurekahost",
						"eureka.client.eureka-server-u-r-l-context=eureka", "eureka.client.eureka-server-port=30000")
				.run(context -> assertThat(output)
						.contains("Cannot get cnames bound to the region:txt.us-east-1.myeurekahost"));
	}

	@Test
	public void eurekaConfigServerInstanceProviderCalled() {
		// FIXME: why do I need to do this? (fails in maven build without it.
		TomcatURLStreamHandlerFactory.disable();
		new SpringApplicationBuilder(TestConfigDiscoveryConfiguration.class)
				.properties("spring.config.use-legacy-processing=true", "spring.cloud.config.discovery.enabled=true",
						"eureka.client.enabled=true",
						"spring.main.sources=" + TestConfigDiscoveryBootstrapConfiguration.class.getName(),
						"logging.level.org.springframework.cloud.netflix.eureka.config=DEBUG")
				.run();
		assertThat(output).contains("eurekaConfigServerInstanceProvider finding instances for configserver")
				.contains("eurekaConfigServerInstanceProvider found 1 instance(s) for configserver");
	}

	private void assertEurekaBeansPresent(AssertableApplicationContext context) {
		assertThat(context).hasSingleBean(EurekaClientConfigBean.class);
		assertThat(context).hasSingleBean(RestTemplateEurekaHttpClient.class);
		assertThat(context).hasSingleBean(ConfigServerInstanceProvider.Function.class);
	}

	private void assertEurekaBeansNotPresent(AssertableApplicationContext context) {
		assertThat(context).doesNotHaveBean(EurekaClientConfigBean.class);
		assertThat(context).doesNotHaveBean(EurekaHttpClient.class);
		assertThat(context).doesNotHaveBean(ConfigServerInstanceProvider.Function.class);
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
			InstanceInfo instanceInfo = InstanceInfo.Builder.newBuilder().setAppName("configserver").build();
			List<InstanceInfo> instanceInfos = Collections.singletonList(instanceInfo);

			Applications applications = mock(Applications.class);
			when(applications.getInstancesByVirtualHostName("configserver")).thenReturn(instanceInfos);

			EurekaHttpResponse<Applications> response = mock(EurekaHttpResponse.class);
			when(response.getStatusCode()).thenReturn(200);
			when(response.getEntity()).thenReturn(applications);

			EurekaHttpClient client = mock(EurekaHttpClient.class);
			when(client.getApplications("us-east-1")).thenReturn(response);
			return client;
		}

	}

}
