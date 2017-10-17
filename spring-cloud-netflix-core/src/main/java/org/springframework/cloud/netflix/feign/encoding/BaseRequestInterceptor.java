/*
 * Copyright 2013-2017 the original author or authors.
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

package org.springframework.cloud.netflix.feign.encoding;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.util.Assert;

/**
 * The base request interceptor.
 *
 * @author Jakub Narloch
 */
public abstract class BaseRequestInterceptor implements RequestInterceptor {

	/**
	 * The encoding properties.
	 */
	private final FeignClientEncodingProperties properties;

	/**
	 * Creates new instance of {@link BaseRequestInterceptor}.
	 *
	 * @param properties the encoding properties
	 */
	protected BaseRequestInterceptor(FeignClientEncodingProperties properties) {
		Assert.notNull(properties, "Properties can not be null");
		this.properties = properties;
	}

	/**
	 * Adds the header if it wasn't yet specified.
	 *
	 * @param requestTemplate the request
	 * @param name			the header name
	 * @param values		  the header values
	 */
	protected void addHeader(RequestTemplate requestTemplate, String name, String... values) {

		if (!requestTemplate.headers().containsKey(name)) {
			requestTemplate.header(name, values);
		}
	}

	protected FeignClientEncodingProperties getProperties() {
		return properties;
	}

}
