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

import java.util.Objects;

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

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EurekaDashboardProperties that = (EurekaDashboardProperties) o;
		return enabled == that.enabled &&
				Objects.equals(path, that.path);
	}

	@Override
	public int hashCode() {
		return Objects.hash(path, enabled);
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("EurekaDashboardProperties{");
		sb.append("path='").append(path).append('\'');
		sb.append(", enabled=").append(enabled);
		sb.append('}');
		return sb.toString();
	}
}
