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

package org.springframework.cloud.netflix.eureka;

import com.netflix.discovery.shared.transport.EurekaTransportConfig;

/**
 * @author Spencer Gibb
 */
public class CloudEurekaTransportConfig implements EurekaTransportConfig {

	private int sessionedClientReconnectIntervalSeconds = 20 * 60;

	private double retryableClientQuarantineRefreshPercentage = 0.66;

	private int bootstrapResolverRefreshIntervalSeconds = 5 * 60;

	private int applicationsResolverDataStalenessThresholdSeconds = 5 * 60;

	private int asyncResolverRefreshIntervalMs = 5 * 60 * 1000;

	private int asyncResolverWarmUpTimeoutMs = 5000;

	private int asyncExecutorThreadPoolSize = 5;

	private String readClusterVip;

	private boolean bootstrapResolverForQuery = true;

	public CloudEurekaTransportConfig() {
	}

	@Override
	public boolean useBootstrapResolverForQuery() {
		return this.bootstrapResolverForQuery;
	}

	public int getSessionedClientReconnectIntervalSeconds() {
		return this.sessionedClientReconnectIntervalSeconds;
	}

	public double getRetryableClientQuarantineRefreshPercentage() {
		return this.retryableClientQuarantineRefreshPercentage;
	}

	public int getBootstrapResolverRefreshIntervalSeconds() {
		return this.bootstrapResolverRefreshIntervalSeconds;
	}

	public int getApplicationsResolverDataStalenessThresholdSeconds() {
		return this.applicationsResolverDataStalenessThresholdSeconds;
	}

	public int getAsyncResolverRefreshIntervalMs() {
		return this.asyncResolverRefreshIntervalMs;
	}

	public int getAsyncResolverWarmUpTimeoutMs() {
		return this.asyncResolverWarmUpTimeoutMs;
	}

	public int getAsyncExecutorThreadPoolSize() {
		return this.asyncExecutorThreadPoolSize;
	}

	public String getReadClusterVip() {
		return this.readClusterVip;
	}

	public boolean isBootstrapResolverForQuery() {
		return this.bootstrapResolverForQuery;
	}

	public void setSessionedClientReconnectIntervalSeconds(
			int sessionedClientReconnectIntervalSeconds) {
		this.sessionedClientReconnectIntervalSeconds = sessionedClientReconnectIntervalSeconds;
	}

	public void setRetryableClientQuarantineRefreshPercentage(
			double retryableClientQuarantineRefreshPercentage) {
		this.retryableClientQuarantineRefreshPercentage = retryableClientQuarantineRefreshPercentage;
	}

	public void setBootstrapResolverRefreshIntervalSeconds(
			int bootstrapResolverRefreshIntervalSeconds) {
		this.bootstrapResolverRefreshIntervalSeconds = bootstrapResolverRefreshIntervalSeconds;
	}

	public void setApplicationsResolverDataStalenessThresholdSeconds(
			int applicationsResolverDataStalenessThresholdSeconds) {
		this.applicationsResolverDataStalenessThresholdSeconds = applicationsResolverDataStalenessThresholdSeconds;
	}

	public void setAsyncResolverRefreshIntervalMs(int asyncResolverRefreshIntervalMs) {
		this.asyncResolverRefreshIntervalMs = asyncResolverRefreshIntervalMs;
	}

	public void setAsyncResolverWarmUpTimeoutMs(int asyncResolverWarmUpTimeoutMs) {
		this.asyncResolverWarmUpTimeoutMs = asyncResolverWarmUpTimeoutMs;
	}

	public void setAsyncExecutorThreadPoolSize(int asyncExecutorThreadPoolSize) {
		this.asyncExecutorThreadPoolSize = asyncExecutorThreadPoolSize;
	}

	public void setReadClusterVip(String readClusterVip) {
		this.readClusterVip = readClusterVip;
	}

	public void setBootstrapResolverForQuery(boolean bootstrapResolverForQuery) {
		this.bootstrapResolverForQuery = bootstrapResolverForQuery;
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof CloudEurekaTransportConfig))
			return false;
		final CloudEurekaTransportConfig other = (CloudEurekaTransportConfig) o;
		if (!other.canEqual((Object) this))
			return false;
		if (this.sessionedClientReconnectIntervalSeconds
				!= other.sessionedClientReconnectIntervalSeconds)
			return false;
		if (Double.compare(this.retryableClientQuarantineRefreshPercentage,
				other.retryableClientQuarantineRefreshPercentage) != 0)
			return false;
		if (this.bootstrapResolverRefreshIntervalSeconds
				!= other.bootstrapResolverRefreshIntervalSeconds)
			return false;
		if (this.applicationsResolverDataStalenessThresholdSeconds
				!= other.applicationsResolverDataStalenessThresholdSeconds)
			return false;
		if (this.asyncResolverRefreshIntervalMs != other.asyncResolverRefreshIntervalMs)
			return false;
		if (this.asyncResolverWarmUpTimeoutMs != other.asyncResolverWarmUpTimeoutMs)
			return false;
		if (this.asyncExecutorThreadPoolSize != other.asyncExecutorThreadPoolSize)
			return false;
		final Object this$readClusterVip = this.readClusterVip;
		final Object other$readClusterVip = other.readClusterVip;
		if (this$readClusterVip == null ?
				other$readClusterVip != null :
				!this$readClusterVip.equals(other$readClusterVip))
			return false;
		if (this.bootstrapResolverForQuery != other.bootstrapResolverForQuery)
			return false;
		return true;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		result = result * PRIME + this.sessionedClientReconnectIntervalSeconds;
		final long $retryableClientQuarantineRefreshPercentage = Double
				.doubleToLongBits(this.retryableClientQuarantineRefreshPercentage);
		result =
				result * PRIME + (int) ($retryableClientQuarantineRefreshPercentage >>> 32
						^ $retryableClientQuarantineRefreshPercentage);
		result = result * PRIME + this.bootstrapResolverRefreshIntervalSeconds;
		result = result * PRIME + this.applicationsResolverDataStalenessThresholdSeconds;
		result = result * PRIME + this.asyncResolverRefreshIntervalMs;
		result = result * PRIME + this.asyncResolverWarmUpTimeoutMs;
		result = result * PRIME + this.asyncExecutorThreadPoolSize;
		final Object $readClusterVip = this.readClusterVip;
		result = result * PRIME + ($readClusterVip == null ?
				0 :
				$readClusterVip.hashCode());
		result = result * PRIME + (this.bootstrapResolverForQuery ? 79 : 97);
		return result;
	}

	protected boolean canEqual(Object other) {
		return other instanceof CloudEurekaTransportConfig;
	}

	public String toString() {
		return "org.springframework.cloud.netflix.eureka.CloudEurekaTransportConfig(sessionedClientReconnectIntervalSeconds="
				+ this.sessionedClientReconnectIntervalSeconds
				+ ", retryableClientQuarantineRefreshPercentage="
				+ this.retryableClientQuarantineRefreshPercentage
				+ ", bootstrapResolverRefreshIntervalSeconds="
				+ this.bootstrapResolverRefreshIntervalSeconds
				+ ", applicationsResolverDataStalenessThresholdSeconds="
				+ this.applicationsResolverDataStalenessThresholdSeconds
				+ ", asyncResolverRefreshIntervalMs="
				+ this.asyncResolverRefreshIntervalMs + ", asyncResolverWarmUpTimeoutMs="
				+ this.asyncResolverWarmUpTimeoutMs + ", asyncExecutorThreadPoolSize="
				+ this.asyncExecutorThreadPoolSize + ", readClusterVip="
				+ this.readClusterVip + ", bootstrapResolverForQuery="
				+ this.bootstrapResolverForQuery + ")";
	}
}
