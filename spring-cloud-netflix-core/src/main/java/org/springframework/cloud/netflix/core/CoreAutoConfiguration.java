/*
 * Copyright 2018-2019 the original author or authors.
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

package org.springframework.cloud.netflix.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.annotation.Configuration;

/**
 * @author Olga Maciaszek-Sharma
 * @since 2.1.0
 * @deprecated Module spring-cloud-netflix-core is deprecated as of 2.1.0, use
 * spring-cloud-netflix-hystrix instead.
 */
@Configuration
@Deprecated
public class CoreAutoConfiguration {

	private static final Log LOG = LogFactory.getLog(CoreAutoConfiguration.class);

	public CoreAutoConfiguration() {
		LOG.warn(
				"This module is deprecated. It will be removed in the next major release. "
						+ "Please use spring-cloud-netflix-hystrix instead.");
	}

}
