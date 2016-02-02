/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.server;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Eureka dashboard (UI).
 *
 * @author Dave Syer
 */
@ConfigurationProperties("eureka.dashboard")
public class EurekaDashboardProperties {

	/**
	 * The path to the Eureka dashboard (relative to the servlet path). Defaults to "/".
	 */
	private String path = "/";

	/**
	 * Flag to enable the Eureka dashboard. Default true.
	 */
	private boolean enabled = true;

	public EurekaDashboardProperties() {
	}

	public String getPath() {
		return this.path;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof EurekaDashboardProperties))
			return false;
		final EurekaDashboardProperties other = (EurekaDashboardProperties) o;
		if (!other.canEqual((Object) this))
			return false;
		final Object this$path = this.path;
		final Object other$path = other.path;
		if (this$path == null ? other$path != null : !this$path.equals(other$path))
			return false;
		if (this.enabled != other.enabled)
			return false;
		return true;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $path = this.path;
		result = result * PRIME + ($path == null ? 0 : $path.hashCode());
		result = result * PRIME + (this.enabled ? 79 : 97);
		return result;
	}

	protected boolean canEqual(Object other) {
		return other instanceof EurekaDashboardProperties;
	}

	public String toString() {
		return "org.springframework.cloud.netflix.eureka.server.EurekaDashboardProperties(path="
				+ this.path + ", enabled=" + this.enabled + ")";
	}
}
