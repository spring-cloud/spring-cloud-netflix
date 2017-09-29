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
import java.util.Objects;
import java.util.Set;

import org.springframework.util.StringUtils;

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

	public Route(String id, String path, String location, String prefix,
		Boolean retryable, Set<String> ignoredHeaders, boolean prefixStripped) {
		this(id, path, location, prefix, retryable, ignoredHeaders);
		this.prefixStripped = prefixStripped;
	}

	private String id;

	private String fullPath;

	private String path;

	private String location;

	private String prefix;

	private Boolean retryable;

	private Set<String> sensitiveHeaders = new LinkedHashSet<>();

	private boolean customSensitiveHeaders;

	private boolean prefixStripped = true;

	public boolean isCustomSensitiveHeaders() {
		return this.customSensitiveHeaders;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFullPath() {
		return fullPath;
	}

	public void setFullPath(String fullPath) {
		this.fullPath = fullPath;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public Boolean getRetryable() {
		return retryable;
	}

	public void setRetryable(Boolean retryable) {
		this.retryable = retryable;
	}

	public Set<String> getSensitiveHeaders() {
		return sensitiveHeaders;
	}

	public void setSensitiveHeaders(Set<String> sensitiveHeaders) {
		this.sensitiveHeaders = sensitiveHeaders;
	}

	public void setCustomSensitiveHeaders(boolean customSensitiveHeaders) {
		this.customSensitiveHeaders = customSensitiveHeaders;
	}

	public boolean isPrefixStripped() {
		return prefixStripped;
	}

	public void setPrefixStripped(boolean prefixStripped) {
		this.prefixStripped = prefixStripped;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Route that = (Route) o;
		return customSensitiveHeaders == that.customSensitiveHeaders &&
				prefixStripped == that.prefixStripped &&
				Objects.equals(id, that.id) &&
				Objects.equals(fullPath, that.fullPath) &&
				Objects.equals(path, that.path) &&
				Objects.equals(location, that.location) &&
				Objects.equals(prefix, that.prefix) &&
				Objects.equals(retryable, that.retryable) &&
				Objects.equals(sensitiveHeaders, that.sensitiveHeaders);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, fullPath, path, location, prefix, retryable,
				sensitiveHeaders, customSensitiveHeaders, prefixStripped);
	}

	@Override
	public String toString() {
		return new StringBuilder("Route{")
				.append("id='").append(id).append("', ")
				.append("fullPath='").append(fullPath).append("', ")
				.append("path='").append(path).append("', ")
				.append("location='").append(location).append("', ")
				.append("prefix='").append(prefix).append("', ")
				.append("retryable=").append(retryable).append(", ")
				.append("sensitiveHeaders=").append(sensitiveHeaders).append(", ")
				.append("customSensitiveHeaders=").append(customSensitiveHeaders).append(", ")
				.append("prefixStripped=").append(prefixStripped)
				.append("}").toString();
	}
}