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

import lombok.Data;

/**
 * @author Spencer Gibb
 */
@Data
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
}
