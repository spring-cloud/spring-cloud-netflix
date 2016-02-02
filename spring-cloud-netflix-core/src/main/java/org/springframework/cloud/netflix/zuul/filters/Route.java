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

import org.springframework.util.StringUtils;

public class Route {

	public Route(String id, String path, String location, String prefix,
			Boolean retryable) {
		this.id = id;
		this.prefix = StringUtils.hasText(prefix) ? prefix : "";
		this.path = path;
		this.fullPath = prefix + path;
		this.location = location;
		this.retryable = retryable;

	}

	private String id;

	private String fullPath;

	private String path;

	private String location;

	private String prefix;

	private Boolean retryable;

	public String getId() {
		return this.id;
	}

	public String getFullPath() {
		return this.fullPath;
	}

	public String getPath() {
		return this.path;
	}

	public String getLocation() {
		return this.location;
	}

	public String getPrefix() {
		return this.prefix;
	}

	public Boolean getRetryable() {
		return this.retryable;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setFullPath(String fullPath) {
		this.fullPath = fullPath;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public void setRetryable(Boolean retryable) {
		this.retryable = retryable;
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Route))
			return false;
		final Route other = (Route) o;
		if (!other.canEqual((Object) this))
			return false;
		final Object this$id = this.id;
		final Object other$id = other.id;
		if (this$id == null ? other$id != null : !this$id.equals(other$id))
			return false;
		final Object this$fullPath = this.fullPath;
		final Object other$fullPath = other.fullPath;
		if (this$fullPath == null ?
				other$fullPath != null :
				!this$fullPath.equals(other$fullPath))
			return false;
		final Object this$path = this.path;
		final Object other$path = other.path;
		if (this$path == null ? other$path != null : !this$path.equals(other$path))
			return false;
		final Object this$location = this.location;
		final Object other$location = other.location;
		if (this$location == null ?
				other$location != null :
				!this$location.equals(other$location))
			return false;
		final Object this$prefix = this.prefix;
		final Object other$prefix = other.prefix;
		if (this$prefix == null ?
				other$prefix != null :
				!this$prefix.equals(other$prefix))
			return false;
		final Object this$retryable = this.retryable;
		final Object other$retryable = other.retryable;
		if (this$retryable == null ?
				other$retryable != null :
				!this$retryable.equals(other$retryable))
			return false;
		return true;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $id = this.id;
		result = result * PRIME + ($id == null ? 0 : $id.hashCode());
		final Object $fullPath = this.fullPath;
		result = result * PRIME + ($fullPath == null ? 0 : $fullPath.hashCode());
		final Object $path = this.path;
		result = result * PRIME + ($path == null ? 0 : $path.hashCode());
		final Object $location = this.location;
		result = result * PRIME + ($location == null ? 0 : $location.hashCode());
		final Object $prefix = this.prefix;
		result = result * PRIME + ($prefix == null ? 0 : $prefix.hashCode());
		final Object $retryable = this.retryable;
		result = result * PRIME + ($retryable == null ? 0 : $retryable.hashCode());
		return result;
	}

	protected boolean canEqual(Object other) {
		return other instanceof Route;
	}

	public String toString() {
		return "org.springframework.cloud.netflix.zuul.filters.Route(id=" + this.id
				+ ", fullPath=" + this.fullPath + ", path=" + this.path + ", location="
				+ this.location + ", prefix=" + this.prefix + ", retryable="
				+ this.retryable + ")";
	}
}