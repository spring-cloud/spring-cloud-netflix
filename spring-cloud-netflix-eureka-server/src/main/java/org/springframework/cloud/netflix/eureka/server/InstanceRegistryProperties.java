/*
 * Copyright 2013-2016 the original author or authors.
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
 *
 */

package org.springframework.cloud.netflix.eureka.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static org.springframework.cloud.netflix.eureka.server.InstanceRegistryProperties.PREFIX;

/**
 * @author Spencer Gibb
 */
@ConfigurationProperties(PREFIX)
public class InstanceRegistryProperties {

	public static final String PREFIX = "eureka.instance.registry";


	/* Default number of expected renews per minute, defaults to 1.
	 * Setting expectedNumberOfRenewsPerMin to non-zero to ensure that even an isolated
	 * server can adjust its eviction policy to the number of registrations (when it's
	 * zero, even a successful registration won't reset the rate threshold in
	 * InstanceRegistry.register()).
	 */
	@Value("${eureka.server.expectedNumberOfRenewsPerMin:1}") // for backwards compatibility
	private int expectedNumberOfRenewsPerMin = 1;

	/** Value used in determining when leases are cancelled, default to 1 for standalone.
	 * Should be set to 0 for peer replicated eurekas */
	@Value("${eureka.server.defaultOpenForTrafficCount:1}") // for backwards compatibility
	private int defaultOpenForTrafficCount = 1;

	public int getExpectedNumberOfRenewsPerMin() {
		return expectedNumberOfRenewsPerMin;
	}

	public void setExpectedNumberOfRenewsPerMin(int expectedNumberOfRenewsPerMin) {
		this.expectedNumberOfRenewsPerMin = expectedNumberOfRenewsPerMin;
	}

	public int getDefaultOpenForTrafficCount() {
		return defaultOpenForTrafficCount;
	}

	public void setDefaultOpenForTrafficCount(int defaultOpenForTrafficCount) {
		this.defaultOpenForTrafficCount = defaultOpenForTrafficCount;
	}
}
