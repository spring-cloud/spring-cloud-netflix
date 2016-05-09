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

package org.springframework.cloud.netflix.zuul.filters;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.util.StringUtils;

import lombok.Data;

@Data
public class Route {

	public Route(String id, String path, String location, String prefix,
			Boolean retryable, Set<String> ignoredHeaders) {
		this.id = id;
		this.prefix = StringUtils.hasText(prefix) ? prefix : "";
		this.path = path;
		this.fullPath = prefix + path;
		this.location = location;
		this.retryable = retryable;
		this.sensitiveHeaders = new LinkedHashSet<>();
		if (ignoredHeaders != null) {
			this.customSensitiveHeaders = true;
			for (String header : ignoredHeaders) {
				this.sensitiveHeaders.add(header.toLowerCase());
			}
		}
	}

	private String id;

	private String fullPath;

	private String path;

	private String location;

	private String prefix;

	private Boolean retryable;

	private Set<String> sensitiveHeaders = new LinkedHashSet<>();

	private boolean customSensitiveHeaders;

	public boolean isCustomSensitiveHeaders() {
		return this.customSensitiveHeaders;
	}

}