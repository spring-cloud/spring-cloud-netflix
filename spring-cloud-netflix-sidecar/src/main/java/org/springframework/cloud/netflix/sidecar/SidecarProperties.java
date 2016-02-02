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

package org.springframework.cloud.netflix.sidecar;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Spencer Gibb
 */
@ConfigurationProperties("sidecar")
public class SidecarProperties {

	private URI healthUri;

	private URI homePageUri;

	private int port;

	public SidecarProperties() {
	}

	public URI getHealthUri() {
		return this.healthUri;
	}

	public URI getHomePageUri() {
		return this.homePageUri;
	}

	public int getPort() {
		return this.port;
	}

	public void setHealthUri(URI healthUri) {
		this.healthUri = healthUri;
	}

	public void setHomePageUri(URI homePageUri) {
		this.homePageUri = homePageUri;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof SidecarProperties))
			return false;
		final SidecarProperties other = (SidecarProperties) o;
		if (!other.canEqual((Object) this))
			return false;
		final Object this$healthUri = this.healthUri;
		final Object other$healthUri = other.healthUri;
		if (this$healthUri == null ?
				other$healthUri != null :
				!this$healthUri.equals(other$healthUri))
			return false;
		final Object this$homePageUri = this.homePageUri;
		final Object other$homePageUri = other.homePageUri;
		if (this$homePageUri == null ?
				other$homePageUri != null :
				!this$homePageUri.equals(other$homePageUri))
			return false;
		if (this.port != other.port)
			return false;
		return true;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $healthUri = this.healthUri;
		result = result * PRIME + ($healthUri == null ? 0 : $healthUri.hashCode());
		final Object $homePageUri = this.homePageUri;
		result = result * PRIME + ($homePageUri == null ? 0 : $homePageUri.hashCode());
		result = result * PRIME + this.port;
		return result;
	}

	protected boolean canEqual(Object other) {
		return other instanceof SidecarProperties;
	}

	public String toString() {
		return "org.springframework.cloud.netflix.sidecar.SidecarProperties(healthUri="
				+ this.healthUri + ", homePageUri=" + this.homePageUri + ", port="
				+ this.port + ")";
	}
}
