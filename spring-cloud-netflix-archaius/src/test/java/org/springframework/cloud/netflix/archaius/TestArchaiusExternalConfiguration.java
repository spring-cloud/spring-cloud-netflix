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

package org.springframework.cloud.netflix.archaius;

import org.apache.commons.configuration.AbstractConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.config.ConcurrentMapConfiguration;

/**
 * @author Alexandru-George Burghelea
 */
@Configuration
public class TestArchaiusExternalConfiguration {

	@Bean
	@Qualifier("dynamicConfiguration")
	public AbstractConfiguration createDynamicConfiguration() {
		ConcurrentMapConfiguration config = new ConcurrentMapConfiguration();
		config.addProperty("db.property","this is a db property");
		config.addProperty("db.second.property","this is another db property");
		return config;
	}

}
