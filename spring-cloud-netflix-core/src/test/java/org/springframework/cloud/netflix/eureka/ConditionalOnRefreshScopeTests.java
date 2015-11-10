/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.netflix.eureka;

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration.ConditionalOnRefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Dave Syer
 *
 */
public class ConditionalOnRefreshScopeTests {

	private ConfigurableApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void refreshScopeIncluded() {
		this.context = new SpringApplicationBuilder(RefreshAutoConfiguration.class,
				Beans.class).web(false).bannerMode(Mode.OFF).run();
		assertNotNull(this.context.getBean(
				org.springframework.cloud.context.scope.refresh.RefreshScope.class));
		assertEquals("foo", this.context.getBean("foo"));
	}

	@Test
	public void refreshScopeNotIncluded() {
		this.context = new SpringApplicationBuilder(Beans.class).web(false)
				.bannerMode(Mode.OFF).run();
		assertFalse(this.context.containsBean("foo"));
	}

	@Configuration
	protected static class Beans {
		@Bean
		@ConditionalOnRefreshScope
		public String foo() {
			return "foo";
		}
	}

}
