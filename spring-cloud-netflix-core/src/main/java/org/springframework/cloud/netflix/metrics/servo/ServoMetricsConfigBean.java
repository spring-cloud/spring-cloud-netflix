/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.cloud.netflix.metrics.servo;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties to configure Servo support.
 *
 * @author Jon Schneider
 */
@ConfigurationProperties("netflix.metrics.servo")
public class ServoMetricsConfigBean {

	/**
	 * Enable the Netflix Servo metrics services. If this flag is off Servo can still be
	 * used by Netflix OSS components, but the Spring Boot metrics collection will be done
	 * with the default services.
	 */
	boolean enabled = true;
	/**
	 * Fully qualified class name for monitor registry used by Servo.
	 */
	String registryClass = "com.netflix.servo.BasicMonitorRegistry";

	/**
	 * When the `ServoMonitorCache` reaches this size, a warning is logged.
	 * This will be useful if you are using string concatenation in RestTemplate urls.
	 */
	int cacheWarningThreshold = 1000;

	public boolean getEnabled() {
		return this.enabled;
	}

	public void isEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getRegistryClass() {
		return this.registryClass;
	}

	public void setRegistryClass(String registryClass) {
		this.registryClass = registryClass;
	}

	public int getCacheWarningThreshold() {
		return cacheWarningThreshold;
	}

	public void setCacheWarningThreshold(int cacheWarningThreshold) {
		this.cacheWarningThreshold = cacheWarningThreshold;
	}
}
