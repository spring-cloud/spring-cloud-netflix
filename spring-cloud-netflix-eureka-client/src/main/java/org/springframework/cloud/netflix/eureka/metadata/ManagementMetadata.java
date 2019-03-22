/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.metadata;

import java.util.Objects;

/**
 * Stores management metadata for Eureka.
 *
 * @author Anastasiia Smirnova
 * @author Ryan Baxter
 */
public class ManagementMetadata {

	private final String healthCheckUrl;

	private final String statusPageUrl;

	private final Integer managementPort;

	private String secureHealthCheckUrl;

	public ManagementMetadata(String healthCheckUrl, String statusPageUrl,
			Integer managementPort) {
		this.healthCheckUrl = healthCheckUrl;
		this.statusPageUrl = statusPageUrl;
		this.managementPort = managementPort;
		this.secureHealthCheckUrl = null;
	}

	public String getHealthCheckUrl() {
		return healthCheckUrl;
	}

	public String getStatusPageUrl() {
		return statusPageUrl;
	}

	public Integer getManagementPort() {
		return managementPort;
	}

	public String getSecureHealthCheckUrl() {
		return secureHealthCheckUrl;
	}

	public void setSecureHealthCheckUrl(String secureHealthCheckUrl) {
		this.secureHealthCheckUrl = secureHealthCheckUrl;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ManagementMetadata that = (ManagementMetadata) o;
		return Objects.equals(healthCheckUrl, that.healthCheckUrl)
				&& Objects.equals(statusPageUrl, that.statusPageUrl)
				&& Objects.equals(managementPort, that.managementPort);
	}

	@Override
	public int hashCode() {
		return Objects.hash(healthCheckUrl, statusPageUrl, managementPort);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("ManagementMetadata{");
		sb.append("healthCheckUrl='").append(healthCheckUrl).append('\'');
		sb.append(", statusPageUrl='").append(statusPageUrl).append('\'');
		sb.append(", managementPort=").append(managementPort);
		sb.append('}');
		return sb.toString();
	}

}
