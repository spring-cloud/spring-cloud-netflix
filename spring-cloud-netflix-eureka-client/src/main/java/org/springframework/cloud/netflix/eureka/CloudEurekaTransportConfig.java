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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * @author Spencer Gibb
 * @author Gregor Zurowski
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

	private String writeClusterVip;

	private boolean bootstrapResolverForQuery = true;

	private String bootstrapResolverStrategy;

	private boolean applicationsResolverUseIp = false;

	@Override
	public boolean useBootstrapResolverForQuery() {
		return this.bootstrapResolverForQuery;
	}

	@Override
	public boolean applicationsResolverUseIp() {
		return this.applicationsResolverUseIp;
	}

	public int getSessionedClientReconnectIntervalSeconds() {
		return sessionedClientReconnectIntervalSeconds;
	}

	public void setSessionedClientReconnectIntervalSeconds(
			int sessionedClientReconnectIntervalSeconds) {
		this.sessionedClientReconnectIntervalSeconds = sessionedClientReconnectIntervalSeconds;
	}

	public double getRetryableClientQuarantineRefreshPercentage() {
		return retryableClientQuarantineRefreshPercentage;
	}

	public void setRetryableClientQuarantineRefreshPercentage(
			double retryableClientQuarantineRefreshPercentage) {
		this.retryableClientQuarantineRefreshPercentage = retryableClientQuarantineRefreshPercentage;
	}

	public int getBootstrapResolverRefreshIntervalSeconds() {
		return bootstrapResolverRefreshIntervalSeconds;
	}

	public void setBootstrapResolverRefreshIntervalSeconds(
			int bootstrapResolverRefreshIntervalSeconds) {
		this.bootstrapResolverRefreshIntervalSeconds = bootstrapResolverRefreshIntervalSeconds;
	}

	public int getApplicationsResolverDataStalenessThresholdSeconds() {
		return applicationsResolverDataStalenessThresholdSeconds;
	}

	public void setApplicationsResolverDataStalenessThresholdSeconds(
			int applicationsResolverDataStalenessThresholdSeconds) {
		this.applicationsResolverDataStalenessThresholdSeconds = applicationsResolverDataStalenessThresholdSeconds;
	}

	public int getAsyncResolverRefreshIntervalMs() {
		return asyncResolverRefreshIntervalMs;
	}

	public void setAsyncResolverRefreshIntervalMs(int asyncResolverRefreshIntervalMs) {
		this.asyncResolverRefreshIntervalMs = asyncResolverRefreshIntervalMs;
	}

	public int getAsyncResolverWarmUpTimeoutMs() {
		return asyncResolverWarmUpTimeoutMs;
	}

	public void setAsyncResolverWarmUpTimeoutMs(int asyncResolverWarmUpTimeoutMs) {
		this.asyncResolverWarmUpTimeoutMs = asyncResolverWarmUpTimeoutMs;
	}

	public int getAsyncExecutorThreadPoolSize() {
		return asyncExecutorThreadPoolSize;
	}

	public void setAsyncExecutorThreadPoolSize(int asyncExecutorThreadPoolSize) {
		this.asyncExecutorThreadPoolSize = asyncExecutorThreadPoolSize;
	}

	public String getReadClusterVip() {
		return readClusterVip;
	}

	public void setReadClusterVip(String readClusterVip) {
		this.readClusterVip = readClusterVip;
	}

	public String getWriteClusterVip() {
		return writeClusterVip;
	}

	public void setWriteClusterVip(String writeClusterVip) {
		this.writeClusterVip = writeClusterVip;
	}

	public boolean isBootstrapResolverForQuery() {
		return bootstrapResolverForQuery;
	}

	public void setBootstrapResolverForQuery(boolean bootstrapResolverForQuery) {
		this.bootstrapResolverForQuery = bootstrapResolverForQuery;
	}

	public String getBootstrapResolverStrategy() {
		return bootstrapResolverStrategy;
	}

	public void setBootstrapResolverStrategy(String bootstrapResolverStrategy) {
		this.bootstrapResolverStrategy = bootstrapResolverStrategy;
	}

	public boolean isApplicationsResolverUseIp() {
		return applicationsResolverUseIp;
	}

	public void setApplicationsResolverUseIp(boolean applicationsResolverUseIp) {
		this.applicationsResolverUseIp = applicationsResolverUseIp;
	}

	@Override
	public boolean equals(Object o) {
		return EqualsBuilder.reflectionEquals(this, o);
	}

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}
