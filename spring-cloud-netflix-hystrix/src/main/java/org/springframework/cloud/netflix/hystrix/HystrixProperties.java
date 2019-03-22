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

package org.springframework.cloud.netflix.hystrix;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Hystrix Servlet.
 *
 * @author Spencer Gibb
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "management.endpoint.hystrix")
public class HystrixProperties {

	/**
	 * Hystrix settings. These are traditionally set using servlet parameters. Refer to
	 * the documentation of Hystrix for more details.
	 */
	private final Map<String, String> config = new HashMap<>();

	public Map<String, String> getConfig() {
		return this.config;
	}

}
