/*
 * Copyright 2015-2019 the original author or authors.
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

package org.springframework.cloud.netflix.eureka;

import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration.ConditionalOnMissingRefreshScope;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration.ConditionalOnRefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 * @author Biju Kunjummen
 */
public class ConditionalOnRefreshScopeTests {

	@Test
	public void refreshScopeIncluded() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class))
				.withUserConfiguration(Beans.class).run(c -> {
					assertThat(c).hasSingleBean(
							org.springframework.cloud.context.scope.refresh.RefreshScope.class);
					assertThat(c.getBean("foo")).isEqualTo("foo");
				});
	}

	@Test
	public void refreshScopeIncludedAndPropertyDisabled() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class))
				.withPropertyValues("eureka.client.refresh.enable=false")
				.withUserConfiguration(Beans.class).run(c -> {
					assertThat(c).hasSingleBean(
							org.springframework.cloud.context.scope.refresh.RefreshScope.class);
					assertThat(c).doesNotHaveBean("foo");
					assertThat(c.getBean("bar")).isEqualTo("bar");
				});
	}

	@Test
	public void refreshScopeNotIncluded() {
		new ApplicationContextRunner().withUserConfiguration(Beans.class).run(c -> {
			assertThat(c).doesNotHaveBean("foo");
			assertThat(c.getBean("bar")).isEqualTo("bar");
		});

		new ApplicationContextRunner().withUserConfiguration(Beans.class)
				.withPropertyValues("eureka.client.refresh.enable=false").run(c -> {
					assertThat(c).doesNotHaveBean("foo");
					assertThat(c.getBean("bar")).isEqualTo("bar");
				});
	}

	@Configuration
	protected static class Beans {

		@Bean
		@ConditionalOnRefreshScope
		public String foo() {
			return "foo";
		}

		@Bean
		@ConditionalOnMissingRefreshScope
		public String bar() {
			return "bar";
		}

	}

}
