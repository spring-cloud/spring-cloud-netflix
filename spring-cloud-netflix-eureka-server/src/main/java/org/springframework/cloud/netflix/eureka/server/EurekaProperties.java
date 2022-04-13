/*
 * Copyright 2013-2022 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.server;

import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Eureka deployment.
 *
 * @author Weix Sun
 */
@ConfigurationProperties("eureka")
public class EurekaProperties {

	/**
	 * Eureka environment. Defaults to "test".
	 */
	private String environment = "test";

	/**
	 * Eureka datacenter. Defaults to "default".
	 */
	private String datacenter = "default";

	public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public String getDatacenter() {
		return datacenter;
	}

	public void setDatacenter(String datacenter) {
		this.datacenter = datacenter;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		EurekaProperties that = (EurekaProperties) o;
		return Objects.equals(datacenter, that.datacenter) && Objects.equals(environment, that.environment);
	}

	@Override
	public int hashCode() {
		return Objects.hash(environment, datacenter);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("EurekaProperties{");
		sb.append("environment='").append(environment).append('\'');
		sb.append(", datacenter=").append(datacenter);
		sb.append('}');
		return sb.toString();
	}

}
