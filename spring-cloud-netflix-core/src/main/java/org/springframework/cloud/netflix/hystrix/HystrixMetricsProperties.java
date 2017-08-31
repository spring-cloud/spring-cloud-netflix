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
 */

package org.springframework.cloud.netflix.hystrix;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

/**
 * @author Venil Noronha
 * @author Gregor Zurowski
 */
@ConfigurationProperties("hystrix.metrics")
public class HystrixMetricsProperties {

	/** Enable Hystrix metrics polling. Defaults to true. */
	private boolean enabled = true;

	/** Interval between subsequent polling of metrics. Defaults to 2000 ms. */
	private Integer pollingIntervalMs = 2000;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Integer getPollingIntervalMs() {
		return pollingIntervalMs;
	}

	public void setPollingIntervalMs(Integer pollingIntervalMs) {
		this.pollingIntervalMs = pollingIntervalMs;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		HystrixMetricsProperties that = (HystrixMetricsProperties) o;
		return enabled == that.enabled &&
				Objects.equals(pollingIntervalMs, that.pollingIntervalMs);
	}

	@Override
	public int hashCode() {
		return Objects.hash(enabled, pollingIntervalMs);
	}

	@Override
	public String toString() {
		return new StringBuilder("HystrixMetricsProperties{")
				.append("enabled=").append(enabled).append(", ")
				.append("pollingIntervalMs=").append(pollingIntervalMs)
				.append("}").toString();
	}
}
