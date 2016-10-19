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

package org.springframework.cloud.netflix.eureka.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.netflix.eureka.EurekaConstants;
import org.springframework.core.env.PropertyResolver;

import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.aws.AwsBindingStrategy;

import lombok.Data;

/**
 * @author Dave Syer
 */
@Data
@ConfigurationProperties(EurekaServerConfigBean.PREFIX)
public class EurekaServerConfigBean implements EurekaServerConfig, EurekaConstants {

	public static final String PREFIX = "eureka.server";

	private static final int MINUTES = 60 * 1000;

	@Autowired(required = false)
	PropertyResolver propertyResolver;

	private String aWSAccessId;

	private String aWSSecretKey;

	private int eIPBindRebindRetries = 3;

	private int eIPBindingRetryIntervalMs = 5 * MINUTES;

	private int eIPBindingRetryIntervalMsWhenUnbound = 1 * MINUTES;

	private boolean enableSelfPreservation = true;

	private double renewalPercentThreshold = 0.85;

	private int renewalThresholdUpdateIntervalMs = 15 * MINUTES;

	private int peerEurekaNodesUpdateIntervalMs = 10 * MINUTES;

	private int numberOfReplicationRetries = 5;

	private int peerEurekaStatusRefreshTimeIntervalMs = 30 * 1000;

	private int waitTimeInMsWhenSyncEmpty = 5 * MINUTES;

	private int peerNodeConnectTimeoutMs = 200;

	private int peerNodeReadTimeoutMs = 200;

	private int peerNodeTotalConnections = 1000;

	private int peerNodeTotalConnectionsPerHost = 500;

	private int peerNodeConnectionIdleTimeoutSeconds = 30;

	private long retentionTimeInMSInDeltaQueue = 3 * MINUTES;

	private long deltaRetentionTimerIntervalInMs = 30 * 1000;

	private long evictionIntervalTimerInMs = 60 * 1000;

	private int aSGQueryTimeoutMs = 300;

	private long aSGUpdateIntervalMs = 5 * MINUTES;

	private long aSGCacheExpiryTimeoutMs = 10 * MINUTES; // defaults to longer than the
															// asg update interval

	private long responseCacheAutoExpirationInSeconds = 180;

	private long responseCacheUpdateIntervalMs = 30 * 1000;

	private boolean useReadOnlyResponseCache = true;

	private boolean disableDelta;

	private long maxIdleThreadInMinutesAgeForStatusReplication = 10;

	private int minThreadsForStatusReplication = 1;

	private int maxThreadsForStatusReplication = 1;

	private int maxElementsInStatusReplicationPool = 10000;

	private boolean syncWhenTimestampDiffers = true;

	private int registrySyncRetries = 0;

	private long registrySyncRetryWaitMs = 30 * 1000;

	private int maxElementsInPeerReplicationPool = 10000;

	private long maxIdleThreadAgeInMinutesForPeerReplication = 15;

	private int minThreadsForPeerReplication = 5;

	private int maxThreadsForPeerReplication = 20;

	private int maxTimeForReplication = 30000;

	private boolean primeAwsReplicaConnections = true;

	private boolean disableDeltaForRemoteRegions;

	private int remoteRegionConnectTimeoutMs = 1000;

	private int remoteRegionReadTimeoutMs = 1000;

	private int remoteRegionTotalConnections = 1000;

	private int remoteRegionTotalConnectionsPerHost = 500;

	private int remoteRegionConnectionIdleTimeoutSeconds = 30;

	private boolean gZipContentFromRemoteRegion = true;

	private Map<String, String> remoteRegionUrlsWithName = new HashMap<>();

	private String[] remoteRegionUrls;

	private Map<String, Set<String>> remoteRegionAppWhitelist;

	private int remoteRegionRegistryFetchInterval = 30;

	private int remoteRegionFetchThreadPoolSize = 20;

	private String remoteRegionTrustStore = "";

	private String remoteRegionTrustStorePassword = "changeit";

	private boolean disableTransparentFallbackToOtherRegion;

	private boolean batchReplication;

	private boolean rateLimiterEnabled = false;

	private boolean rateLimiterThrottleStandardClients = false;

	private Set<String> rateLimiterPrivilegedClients = Collections.emptySet();

	private int rateLimiterBurstSize = 10;

	private int rateLimiterRegistryFetchAverageRate = 500;

	private int rateLimiterFullFetchAverageRate = 100;

	private boolean logIdentityHeaders = true;

	private String listAutoScalingGroupsRoleName = "ListAutoScalingGroups";

	private boolean enableReplicatedRequestCompression = false;

	private String jsonCodecName;

	private String xmlCodecName;

	private int route53BindRebindRetries = 3;

	private int route53BindingRetryIntervalMs = 5 * MINUTES;

	private long route53DomainTTL = 30;

	private AwsBindingStrategy bindingStrategy = AwsBindingStrategy.EIP;

	private int minAvailableInstancesForPeerReplication = -1;

	@Override
	public boolean shouldEnableSelfPreservation() {
		return this.enableSelfPreservation;
	}

	@Override
	public boolean shouldDisableDelta() {
		return this.disableDelta;
	}

	@Override
	public boolean shouldSyncWhenTimestampDiffers() {
		return this.syncWhenTimestampDiffers;
	}

	@Override
	public boolean shouldPrimeAwsReplicaConnections() {
		return this.primeAwsReplicaConnections;
	}

	@Override
	public boolean shouldDisableDeltaForRemoteRegions() {
		return this.disableDeltaForRemoteRegions;
	}

	@Override
	public boolean shouldGZipContentFromRemoteRegion() {
		return this.gZipContentFromRemoteRegion;
	}

	@Override
	public Set<String> getRemoteRegionAppWhitelist(String regionName) {
		return this.remoteRegionAppWhitelist
				.get(regionName == null ? "global" : regionName.trim().toLowerCase());
	}

	@Override
	public boolean disableTransparentFallbackToOtherRegion() {
		return this.disableTransparentFallbackToOtherRegion;
	}

	@Override
	public boolean shouldBatchReplication() {
		return this.batchReplication;
	}

	@Override
	public boolean shouldLogIdentityHeaders() {
		return this.logIdentityHeaders;
	}

	@Override
	public String getJsonCodecName() {
		return this.jsonCodecName;
	}

	@Override
	public String getXmlCodecName() {
		return this.xmlCodecName;
	}

	@Override
	public boolean shouldUseReadOnlyResponseCache() {
		return this.useReadOnlyResponseCache;
	}

	@Override
	public boolean shouldEnableReplicatedRequestCompression() {
		return this.enableReplicatedRequestCompression;
	}

	@Override
	public String getExperimental(String name) {
		if (this.propertyResolver != null) {
			return this.propertyResolver.getProperty(PREFIX + ".experimental." + name,
					String.class, null);
		}
		return null;
	}

	@Override
	public int getHealthStatusMinNumberOfAvailablePeers() {
		return this.minAvailableInstancesForPeerReplication;
	}
}
