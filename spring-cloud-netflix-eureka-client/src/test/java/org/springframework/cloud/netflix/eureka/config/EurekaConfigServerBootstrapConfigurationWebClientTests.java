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

import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.config.client.ConfigServerInstanceProvider;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.http.WebClientEurekaHttpClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Spencer Gibb
 */
public class EurekaConfigServerBootstrapConfigurationWebClientTests {

	@Test
	public void properBeansCreatedWhenEnabled() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations
						.of(EurekaConfigServerBootstrapConfiguration.class))
				.withPropertyValues("spring.cloud.config.discovery.enabled=true")
				.run(context -> {
					assertThat(context).hasSingleBean(EurekaClientConfigBean.class);
					assertThat(context).hasSingleBean(WebClientEurekaHttpClient.class);
					assertThat(context)
							.hasSingleBean(ConfigServerInstanceProvider.Function.class);
				});
	}

}
