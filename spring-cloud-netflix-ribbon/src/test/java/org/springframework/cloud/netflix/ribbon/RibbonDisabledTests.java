/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.netflix.ribbon;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.test.ClassPathExclusions;
import org.springframework.cloud.test.ModifiedClassPathRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ryan Baxter
 * @author Biju Kunjummen
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions({ "ribbon-{version:\\d.*}.jar" })
public class RibbonDisabledTests {

	@Test
	public void testRibbonDisabled() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(RibbonAutoConfiguration.class))
				.run(context -> {
					assertThat(context.getBeanNamesForType(SpringClientFactory.class))
							.hasSize(0);
				});
	}

}
